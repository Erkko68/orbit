package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.ACTION_REOPEN
import com.orbit.app.data.mapper.AssignmentDocument
import com.orbit.app.data.mapper.ChecklistDocument
import com.orbit.app.data.mapper.CompletionDocument
import com.orbit.app.data.mapper.CostDocument
import com.orbit.app.data.mapper.EffortDocument
import com.orbit.app.data.mapper.EntryDocument
import com.orbit.app.data.mapper.ModulesDocument
import com.orbit.app.data.mapper.RecurrenceDocument
import com.orbit.app.data.mapper.ReminderDocument
import com.orbit.app.data.mapper.STATUS_DONE
import com.orbit.app.data.mapper.activityDocument
import com.orbit.app.data.mapper.STATUS_OPEN
import com.orbit.app.data.mapper.ScheduleDocument
import com.orbit.app.data.mapper.StoredItem
import com.orbit.app.data.mapper.assignmentDocument
import com.orbit.app.data.mapper.checklistDocument
import com.orbit.app.data.mapper.costDocument
import com.orbit.app.data.mapper.expenseDocument
import com.orbit.app.data.mapper.itemDocument
import com.orbit.app.data.mapper.mapItem
import com.orbit.app.data.mapper.moduleKey
import com.orbit.app.data.mapper.recurrenceDocument
import com.orbit.app.data.mapper.reminderDocument
import com.orbit.app.data.mapper.scheduleDocument
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Cost
import com.orbit.app.domain.model.Effort
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.usecase.itemCostExpense
import com.orbit.app.domain.usecase.nextTurn
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FieldsAndValuesUpdateDSL
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Items in Firestore (ADR 0002), read through a live listener that Firestore's offline cache answers
 * first. Every write is narrow: one core field, one module or one checklist entry, by field path, so
 * concurrent edits merge and keys written by a newer build survive.
 */
class FirestoreItemRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
) : ItemRepository {
    /** Refused while a space made a moment ago is not on the server yet, so asked again for a few seconds. */
    override fun observeItems(spaceId: String): Flow<List<Item>> =
        items(spaceId).orderBy(CREATED_AT).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull(::toItem)
        }.flowOn(Dispatchers.Default).retryWhileRefused()

    /** ISO dates sort as text, so "up to [until]" is a range on the stored string (ADR 0002). */
    override fun observeAgenda(spaceId: String, until: LocalDate): Flow<List<Item>> =
        items(spaceId).where {
            all(
                FieldPath(STATUS) equalTo STATUS_OPEN,
                FieldPath(MODULES, moduleKey(ModuleType.SCHEDULE), "date") lessThanOrEqualTo until.toString(),
            )
        }.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull(::toItem)
        }.flowOn(Dispatchers.Default).retryWhileRefused()

    /** The item and its entry in the history go in one batch (#83). */
    override suspend fun create(spaceId: String, item: NewItem): String {
        val userId = auth.requireUser().id
        val document = items(spaceId).document
        val batch = firestore.batch()
        batch.set(document, itemDocument(spaceId, item, userId)) { encodeDefaults = false }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.CREATE, ActivityTarget.ITEM, document.id, item.title),
        )
        writeLocally("Item ${document.id}") { batch.commit() }
        return document.id
    }

    /** Its entry in the history keeps every field the delete takes away, read from the cache the screen keeps. */
    override suspend fun delete(spaceId: String, itemId: String) {
        val userId = auth.requireUser().id
        val document = items(spaceId).document(itemId)
        val before = document.get(Source.CACHE)
        val batch = firestore.batch()
        batch.delete(document)
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.DELETE, ActivityTarget.ITEM, itemId, titleOf(before), removed(before)),
        )
        writeLocally("Deleting item $itemId") { batch.commit() }
    }

    override suspend fun deleteProposed(spaceId: String, itemIds: List<String>) {
        val userId = auth.requireUser().id
        val batch = firestore.batch()
        val planId = firestore.collection(SPACES).document(spaceId).collection(ACTIVITY).document.id
        itemIds.distinct().forEach { itemId ->
            val document = items(spaceId).document(itemId)
            val before = document.get(Source.CACHE)
            batch.delete(document)
            batch.logActivity(
                firestore, spaceId,
                activityDocument(
                    userId, ActivityAction.DELETE, ActivityTarget.ITEM, itemId, titleOf(before), removed(before), planId,
                ),
            )
        }
        writeLocally("Deleting ${itemIds.size} items the assistant proposed") { batch.commit() }
    }

    override suspend fun rename(spaceId: String, itemId: String, title: String) =
        update(spaceId, itemId, { mapOf(it.previous<String>("title")) }, title) { FieldPath("title") to title }

    override suspend fun setDescription(spaceId: String, itemId: String, description: String?) =
        update(spaceId, itemId, { before ->
            listOfNotNull(
                before.previous<String>("description"),
                before.previous<String>("details").takeIf { before.field<String>("details") != null },
            ).toMap()
        }) {
            FieldPath("description") to (description ?: FieldValue.delete)
            FieldPath("details") to FieldValue.delete
        }

    override suspend fun setModule(spaceId: String, itemId: String, module: Module) =
        update(spaceId, itemId, { mapOf(it.previousModule(module.type)) }) {
            val path = FieldPath(MODULES, moduleKey(module.type))
            when (module) {
                is Schedule -> path.to(ScheduleDocument.serializer(), scheduleDocument(module))
                is Recurrence -> path.to(RecurrenceDocument.serializer(), recurrenceDocument(module))
                is Assignment -> path.to(AssignmentDocument.serializer(), assignmentDocument(module))
                is Checklist -> path.to(ChecklistDocument.serializer(), checklistDocument(module))
                is Reminder -> path.to(ReminderDocument.serializer(), reminderDocument(module))
                is Cost -> path.to(CostDocument.serializer(), costDocument(module))
                is Effort -> path.to(EffortDocument.serializer(), EffortDocument(module.points))
            }
        }

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) =
        update(spaceId, itemId, { mapOf(it.previousModule(type)) }) {
            FieldPath(MODULES, moduleKey(type)) to FieldValue.delete
        }

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) =
        update(spaceId, itemId, { mapOf(it.previous<Boolean>("${entryKey(entryId)}.checked")) }) {
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "checked") to checked
        }

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) {
        val entryId = items(spaceId).document.id
        update(spaceId, itemId, { mapOf(it.previous<EntryDocument>(entryKey(entryId))) }) {
            entryPath(entryId).to(EntryDocument.serializer(), EntryDocument(text, false, quantity, order))
        }
    }

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) =
        update(spaceId, itemId, { before ->
            mapOf(
                before.previous<String>("${entryKey(entryId)}.text"),
                before.previous<Int>("${entryKey(entryId)}.quantity"),
            )
        }) {
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "text") to text
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "quantity") to
                (quantity ?: FieldValue.delete)
        }

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) =
        update(spaceId, itemId, { mapOf(it.previous<EntryDocument>(entryKey(entryId))) }) {
            entryPath(entryId) to FieldValue.delete
        }

    override fun observeCompletions(spaceId: String, itemId: String): Flow<List<Completion>> =
        completions(spaceId)
            .where { "itemId" equalTo itemId }
            .orderBy(COMPLETED_AT, Direction.DESCENDING)
            .limit(LATEST_COMPLETIONS)
            .snapshots.map { snapshot -> snapshot.documents.mapNotNull(::toCompletion) }
            .flowOn(Dispatchers.Default)
            .retryWhileRefused()

    override fun observeMemberCompletions(spaceId: String, userId: String): Flow<List<Completion>> =
        completions(spaceId)
            .where { COMPLETED_BY equalTo userId }
            .orderBy(COMPLETED_AT, Direction.DESCENDING)
            .limit(LATEST_COMPLETIONS)
            .snapshots.map { snapshot -> snapshot.documents.mapNotNull(::toCompletion) }
            .flowOn(Dispatchers.Default)
            .retryWhileRefused()

    override fun observeRecentCompletions(spaceId: String, since: Instant): Flow<List<Completion>> =
        completions(spaceId)
            .where { COMPLETED_AT greaterThanOrEqualTo Timestamp(since.epochSeconds, since.nanosecondsOfSecond) }
            .snapshots.map { snapshot -> snapshot.documents.mapNotNull(::toCompletion) }
            .flowOn(Dispatchers.Default)
            .retryWhileRefused()

    /**
     * The item, its completion and the expense of its cost go in one batch, so the history never misses one nor has one
     * too many, and a charge never lands without its completion (ADR 0014). The activity history (#83) gets the
     * completion, with the date and turn a roll-forward moves, and the charge. The item and the members come from the
     * cache the screen's listeners keep, so completing still works offline.
     */
    override suspend fun complete(spaceId: String, itemId: String, next: Schedule?) {
        val userId = auth.requireUser().id
        val before = items(spaceId).document(itemId).get(Source.CACHE)
        val item = checkNotNull(toItem(before)) { "No item $itemId" }
        val expense = item.modules.cost?.let {
            val space = firestore.collection(SPACES).document(spaceId).get(Source.CACHE)
            val members = checkNotNull(space.field<List<String>>("memberIds")) { "No members in $spaceId" }
            itemCostExpense(item, members, Clock.System.todayIn(TimeZone.currentSystemDefault()))
        }
        // A rotation passes the item on with the roll-forward, to whoever is not away on the next occurrence.
        val rotation = item.modules.assignment as? Assignment.Rotation
        val turn = if (next != null && rotation != null) {
            val members = firestore.collection(SPACES).document(spaceId).collection(MEMBERS).get(Source.CACHE)
            nextTurn(rotation, members.documents.map { it.toMember() }, next.date)
        } else {
            null
        }
        val charge = firestore.collection(SPACES).document(spaceId).collection(EXPENSES).document
        val batch = firestore.batch()
        batch.updateFields(items(spaceId).document(itemId)) {
            encodeDefaults = false
            if (next != null) {
                FieldPath(MODULES, moduleKey(ModuleType.SCHEDULE)).to(ScheduleDocument.serializer(), scheduleDocument(next))
                // The next occurrence is charged again.
                FieldPath(COST_EXPENSE_ID) to FieldValue.delete
                FieldPath(EFFORT_RECORDED) to FieldValue.delete
                if (turn != null) FieldPath(MODULES, moduleKey(ModuleType.ASSIGNMENT), "assigneeIds") to listOf(turn)
            } else {
                FieldPath(STATUS) to STATUS_DONE
                FieldPath(COMPLETED_BY) to userId
                FieldPath(COMPLETED_AT) to FieldValue.serverTimestamp
                if (expense != null) FieldPath(COST_EXPENSE_ID) to charge.id
                FieldPath(EFFORT_RECORDED) to true
            }
        }
        val entry = CompletionDocument(
            itemId, userId, Timestamp.ServerTimestamp,
            effortPoints = item.modules.effort?.points,
            effortCredited = !item.effortRecorded,
        )
        batch.set(completions(spaceId).document, entry) {
            encodeDefaults = false
        }
        val moved = if (next == null) {
            emptyMap()
        } else {
            listOfNotNull(
                before.previous<ScheduleDocument>("$MODULES.${moduleKey(ModuleType.SCHEDULE)}"),
                turn?.let { before.previous<List<String>>("$MODULES.${moduleKey(ModuleType.ASSIGNMENT)}.assigneeIds") },
            ).toMap()
        }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.COMPLETE, ActivityTarget.ITEM, itemId, item.title, moved),
        )
        if (expense != null) {
            batch.set(charge, expenseDocument(spaceId, expense, userId)) { encodeDefaults = false }
            batch.logActivity(
                firestore, spaceId,
                activityDocument(userId, ActivityAction.CREATE, ActivityTarget.EXPENSE, charge.id, expense.title),
            )
        }
        writeLocally("Completing item $itemId") { batch.commit() }
    }

    /** Like [complete], the item, the entry in its history and the one in the space's activity go in one batch. */
    override suspend fun reopen(spaceId: String, itemId: String) {
        val userId = auth.requireUser().id
        val before = items(spaceId).document(itemId).get(Source.CACHE)
        val batch = firestore.batch()
        batch.updateFields(items(spaceId).document(itemId)) {
            FieldPath(STATUS) to STATUS_OPEN
            FieldPath(COMPLETED_BY) to FieldValue.delete
            FieldPath(COMPLETED_AT) to FieldValue.delete
            FieldPath(EFFORT_RECORDED) to true
        }
        val entry = CompletionDocument(itemId, userId, Timestamp.ServerTimestamp, ACTION_REOPEN)
        batch.set(completions(spaceId).document, entry) { encodeDefaults = false }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(userId, ActivityAction.REOPEN, ActivityTarget.ITEM, itemId, titleOf(before)),
        )
        writeLocally("Reopening item $itemId") { batch.commit() }
    }

    private fun items(spaceId: String) = firestore.collection(SPACES).document(spaceId).collection(ITEMS)

    private fun completions(spaceId: String) = firestore.collection(SPACES).document(spaceId).collection(COMPLETIONS)

    private fun toCompletion(document: DocumentSnapshot): Completion? {
        val itemId = document.field<String>("itemId")
        val completedBy = document.field<String>(COMPLETED_BY)
        val completedAt = document.instant(COMPLETED_AT)
        if (itemId == null || completedBy == null || completedAt == null) {
            Logger.w { "Skipping the malformed completion ${document.id}" }
            return null
        }
        return Completion(
            document.id, itemId, completedBy, completedAt, document.field<String>("action") == ACTION_REOPEN,
            document.field<Int>("effortPoints")?.takeIf { it in Effort.POINTS },
            document.field<Boolean>("effortCredited") != false,
        )
    }

    private fun entryPath(entryId: String) = FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId)

    /** The dotted path of a checklist entry, as the activity history names the fields it changed. */
    private fun entryKey(entryId: String) = "$MODULES.${moduleKey(ModuleType.CHECKLIST)}.entries.$entryId"

    /**
     * One narrow edit and its entry in the history, in one batch (#83). The entry keeps the [changes] the write makes,
     * read from the item as the cache holds it before the write, and its title as the write leaves it.
     */
    private suspend fun update(
        spaceId: String,
        itemId: String,
        changes: (DocumentSnapshot) -> Map<String, String>,
        title: String? = null,
        fields: FieldsAndValuesUpdateDSL.() -> Unit,
    ) {
        val userId = auth.requireUser().id
        val document = items(spaceId).document(itemId)
        val before = document.get(Source.CACHE)
        val batch = firestore.batch()
        batch.updateFields(document) {
            encodeDefaults = false
            fields()
        }
        batch.logActivity(
            firestore, spaceId,
            activityDocument(
                userId, ActivityAction.EDIT, ActivityTarget.ITEM, itemId, title ?: titleOf(before), changes(before),
            ),
        )
        writeLocally("A change to item $itemId") { batch.commit() }
    }

    private fun titleOf(document: DocumentSnapshot): String = document.field<String>("title").orEmpty()

    /** The module of [type] as it was, under its path. */
    private fun DocumentSnapshot.previousModule(type: ModuleType): Pair<String, String> {
        val path = "$MODULES.${moduleKey(type)}"
        return when (type) {
            ModuleType.SCHEDULE -> previous<ScheduleDocument>(path)
            ModuleType.RECURRENCE -> previous<RecurrenceDocument>(path)
            ModuleType.ASSIGNMENT -> previous<AssignmentDocument>(path)
            ModuleType.CHECKLIST -> previous<ChecklistDocument>(path)
            ModuleType.REMINDER -> previous<ReminderDocument>(path)
            ModuleType.COST -> previous<CostDocument>(path)
            ModuleType.EFFORT -> previous<EffortDocument>(path)
        }
    }

    /** Every field a delete takes away that undo (#84) needs to put the item back. Only `createdAt` is the server's. */
    private fun removed(document: DocumentSnapshot): Map<String, String> = listOf(
        document.previous<String>("title"),
        document.previous<String>("description"),
        document.previous<String>("details"),
        document.previous<String>(STATUS),
        document.previous<String>("createdBy"),
        document.previous<String>(COMPLETED_BY),
        MODULES to activityJson.encodeToString(ModulesDocument.serializer(), modulesOf(document)),
        document.previous<String>(COST_EXPENSE_ID),
        document.previous<Boolean>(EFFORT_RECORDED),
    ).filterNot { it.second == "null" }.toMap()

    /** Read module by module, so one this build cannot read is left out instead of losing them all. */
    private fun modulesOf(document: DocumentSnapshot) = ModulesDocument(
            schedule = document.field("$MODULES.${moduleKey(ModuleType.SCHEDULE)}"),
            recurrence = document.field("$MODULES.${moduleKey(ModuleType.RECURRENCE)}"),
            assignment = document.field("$MODULES.${moduleKey(ModuleType.ASSIGNMENT)}"),
            checklist = document.field("$MODULES.${moduleKey(ModuleType.CHECKLIST)}"),
            reminder = document.field("$MODULES.${moduleKey(ModuleType.REMINDER)}"),
            cost = document.field("$MODULES.${moduleKey(ModuleType.COST)}"),
            effort = document.field("$MODULES.${moduleKey(ModuleType.EFFORT)}"),
        )

    private fun toItem(document: DocumentSnapshot): Item? {
        val modules = modulesOf(document)
        val stored = StoredItem(
            title = document.field("title"),
            description = document.field("description"),
            details = document.field("details"),
            status = document.field(STATUS),
            createdBy = document.field("createdBy"),
            createdAt = document.instant(CREATED_AT),
            completedBy = document.field(COMPLETED_BY),
            completedAt = document.instant(COMPLETED_AT),
            // Only the keys: Unit decodes from any value, so a module this build does not know still lists.
            moduleKeys = document.field<Map<String, Unit>>(MODULES)?.keys.orEmpty(),
            modules = modules,
            costExpenseId = document.field(COST_EXPENSE_ID),
            effortRecorded = document.field<Boolean>(EFFORT_RECORDED) == true,
        )
        return mapItem(document.id, stored).also {
            if (it == null) Logger.w { "Skipping the malformed item ${document.id}" }
        }
    }

    private companion object {
        const val MODULES = "modules"
        const val STATUS = "status"
        const val CREATED_AT = "createdAt"
        const val COMPLETED_BY = "completedBy"
        const val COMPLETED_AT = "completedAt"
        const val COST_EXPENSE_ID = "costExpenseId"
        const val EFFORT_RECORDED = "effortRecorded"
        const val LATEST_COMPLETIONS = 10
    }
}
