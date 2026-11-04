package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.AssignmentDocument
import com.orbit.app.data.mapper.ChecklistDocument
import com.orbit.app.data.mapper.ACTION_REOPEN
import com.orbit.app.data.mapper.CompletionDocument
import com.orbit.app.data.mapper.EntryDocument
import com.orbit.app.data.mapper.ModulesDocument
import com.orbit.app.data.mapper.RecurrenceDocument
import com.orbit.app.data.mapper.ReminderDocument
import com.orbit.app.data.mapper.STATUS_DONE
import com.orbit.app.data.mapper.STATUS_OPEN
import com.orbit.app.data.mapper.ScheduleDocument
import com.orbit.app.data.mapper.StoredItem
import com.orbit.app.data.mapper.assignmentDocument
import com.orbit.app.data.mapper.checklistDocument
import com.orbit.app.data.mapper.itemDocument
import com.orbit.app.data.mapper.mapItem
import com.orbit.app.data.mapper.moduleKey
import com.orbit.app.data.mapper.recurrenceDocument
import com.orbit.app.data.mapper.reminderDocument
import com.orbit.app.data.mapper.scheduleDocument
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FieldsAndValuesUpdateDSL
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

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

    override suspend fun create(spaceId: String, item: NewItem): String {
        val document = items(spaceId).document
        val data = itemDocument(spaceId, item, auth.requireUser().id)
        writeLocally("Item ${document.id}") { document.set(data) { encodeDefaults = false } }
        return document.id
    }

    override suspend fun delete(spaceId: String, itemId: String) {
        val document = items(spaceId).document(itemId)
        writeLocally("Deleting item $itemId") { document.delete() }
    }

    override suspend fun rename(spaceId: String, itemId: String, title: String) =
        update(spaceId, itemId) { FieldPath("title") to title }

    override suspend fun setDescription(spaceId: String, itemId: String, description: String?) =
        update(spaceId, itemId) {
            FieldPath("description") to (description ?: FieldValue.delete)
            FieldPath("details") to FieldValue.delete
        }

    override suspend fun setModule(spaceId: String, itemId: String, module: Module) =
        update(spaceId, itemId) {
            val path = FieldPath(MODULES, moduleKey(module.type))
            when (module) {
                is Schedule -> path.to(ScheduleDocument.serializer(), scheduleDocument(module))
                is Recurrence -> path.to(RecurrenceDocument.serializer(), recurrenceDocument(module))
                is Assignment -> path.to(AssignmentDocument.serializer(), assignmentDocument(module))
                is Checklist -> path.to(ChecklistDocument.serializer(), checklistDocument(module))
                is Reminder -> path.to(ReminderDocument.serializer(), reminderDocument(module))
            }
        }

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) =
        update(spaceId, itemId) { FieldPath(MODULES, moduleKey(type)) to FieldValue.delete }

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) =
        update(spaceId, itemId) {
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "checked") to checked
        }

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) {
        val entryId = items(spaceId).document.id
        update(spaceId, itemId) {
            entryPath(entryId).to(EntryDocument.serializer(), EntryDocument(text, false, quantity, order))
        }
    }

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) =
        update(spaceId, itemId) {
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "text") to text
            FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId, "quantity") to
                (quantity ?: FieldValue.delete)
        }

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) =
        update(spaceId, itemId) { entryPath(entryId) to FieldValue.delete }

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

    /** The item and its completion go in one batch, so the history never misses one nor has one too many. */
    override suspend fun complete(spaceId: String, itemId: String) {
        val userId = auth.requireUser().id
        val batch = firestore.batch()
        batch.updateFields(items(spaceId).document(itemId)) {
            FieldPath(STATUS) to STATUS_DONE
            FieldPath(COMPLETED_BY) to userId
            FieldPath(COMPLETED_AT) to FieldValue.serverTimestamp
        }
        batch.set(completions(spaceId).document, CompletionDocument(itemId, userId, Timestamp.ServerTimestamp)) {
            encodeDefaults = false
        }
        writeLocally("Completing item $itemId") { batch.commit() }
    }

    /** Like [complete], the item and the entry in its history go in one batch. */
    override suspend fun reopen(spaceId: String, itemId: String) {
        val userId = auth.requireUser().id
        val batch = firestore.batch()
        batch.updateFields(items(spaceId).document(itemId)) {
            FieldPath(STATUS) to STATUS_OPEN
            FieldPath(COMPLETED_BY) to FieldValue.delete
            FieldPath(COMPLETED_AT) to FieldValue.delete
        }
        val entry = CompletionDocument(itemId, userId, Timestamp.ServerTimestamp, ACTION_REOPEN)
        batch.set(completions(spaceId).document, entry) { encodeDefaults = false }
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
        return Completion(document.id, itemId, completedBy, completedAt, document.field<String>("action") == ACTION_REOPEN)
    }

    private fun entryPath(entryId: String) = FieldPath(MODULES, moduleKey(ModuleType.CHECKLIST), "entries", entryId)

    private suspend fun update(spaceId: String, itemId: String, fields: FieldsAndValuesUpdateDSL.() -> Unit) {
        val document = items(spaceId).document(itemId)
        writeLocally("A change to item $itemId") {
            document.updateFields {
                encodeDefaults = false
                fields()
            }
        }
    }

    private fun toItem(document: DocumentSnapshot): Item? {
        val modules = ModulesDocument(
            schedule = document.field("$MODULES.${moduleKey(ModuleType.SCHEDULE)}"),
            recurrence = document.field("$MODULES.${moduleKey(ModuleType.RECURRENCE)}"),
            assignment = document.field("$MODULES.${moduleKey(ModuleType.ASSIGNMENT)}"),
            checklist = document.field("$MODULES.${moduleKey(ModuleType.CHECKLIST)}"),
            reminder = document.field("$MODULES.${moduleKey(ModuleType.REMINDER)}"),
        )
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
        const val LATEST_COMPLETIONS = 10
    }
}
