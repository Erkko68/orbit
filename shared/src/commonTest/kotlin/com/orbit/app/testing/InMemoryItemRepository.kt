package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.itemCostExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val LATEST_COMPLETIONS = 10

/** Items in memory, with ids in creation order across spaces. */
class InMemoryItemRepository(
    private val auth: AuthRepository,
    private val expenses: ExpenseRepository? = null,
    private val spaces: SpaceRepository? = null,
) : ItemRepository {
    /** The items of each space, by its id. */
    private val items = MutableStateFlow(emptyMap<String, List<Item>>())

    /** The completions of each space, newest first. */
    private val completions = MutableStateFlow(emptyMap<String, List<Completion>>())
    private var created = 0
    private var createdEntries = 0

    override fun observeItems(spaceId: String): Flow<List<Item>> = items.map { it[spaceId].orEmpty() }

    override fun observeAgenda(spaceId: String, until: LocalDate): Flow<List<Item>> =
        observeItems(spaceId).map { all ->
            all.filter { item ->
                val date = item.modules.schedule?.date
                item.status is ItemStatus.Open && date != null && date <= until
            }
        }

    override suspend fun create(spaceId: String, item: NewItem): String {
        val id = "item-${++created}"
        val stored = Item(
            id = id,
            title = item.title,
            description = item.description,
            status = ItemStatus.Open,
            createdBy = auth.requireUser().id,
            createdAt = Clock.System.now(),
            modules = item.modules,
        )
        items.update { it + (spaceId to it[spaceId].orEmpty() + stored) }
        return id
    }

    override suspend fun delete(spaceId: String, itemId: String) {
        items.update { all -> all + (spaceId to all[spaceId].orEmpty().filterNot { it.id == itemId }) }
    }

    override suspend fun rename(spaceId: String, itemId: String, title: String) =
        edit(spaceId, itemId) { it.copy(title = title) }

    override suspend fun setDescription(spaceId: String, itemId: String, description: String?) =
        edit(spaceId, itemId) { it.copy(description = description) }


    override suspend fun setModule(spaceId: String, itemId: String, module: Module) =
        edit(spaceId, itemId) { it.copy(modules = it.modules.with(module)) }

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) =
        edit(spaceId, itemId) { it.copy(modules = it.modules.without(type)) }

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) {
        changeChecklist(spaceId, itemId) { entries ->
            check(entries.any { it.id == entryId }) { "No entry $entryId" }
            entries.map { if (it.id == entryId) it.copy(checked = checked) else it }
        }
    }

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) =
        edit(spaceId, itemId) { item ->
            val checklist = checkNotNull(item.modules.checklist) { "Item $itemId has no checklist" }
            while (checklist.entries.any { it.id == "entry-${createdEntries + 1}" }) createdEntries++
            val entry = ChecklistEntry("entry-${++createdEntries}", text, quantity = quantity, order = order)
            item.copy(modules = item.modules.with(checklist.copy(entries = checklist.entries + entry)))
        }

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) =
        editEntry(spaceId, itemId, entryId) { it.copy(text = text, quantity = quantity) }

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) {
        changeChecklist(spaceId, itemId) { entries ->
            check(entries.any { it.id == entryId }) { "No entry $entryId" }
            entries.filterNot { it.id == entryId }
        }
    }

    private suspend fun changeChecklist(
        spaceId: String,
        itemId: String,
        change: (List<ChecklistEntry>) -> List<ChecklistEntry>
    ) {
        val item = observeItems(spaceId).first().first { it.id == itemId }
        val checklist = checkNotNull(item.modules.checklist)
        val entries = change(checklist.entries)
        val updated = item.copy(modules = item.modules.with(checklist.copy(entries = entries)))
        if (item.modules.cost != null && entries.isNotEmpty() && entries.all { it.checked } && item.status is ItemStatus.Open) {
            completeItem(spaceId, updated, null)
        } else edit(spaceId, itemId) { updated }
    }

    override fun observeCompletions(spaceId: String, itemId: String): Flow<List<Completion>> =
        completions.map { all -> all[spaceId].orEmpty().filter { it.itemId == itemId }.take(LATEST_COMPLETIONS) }

    override fun observeMemberCompletions(spaceId: String, userId: String): Flow<List<Completion>> =
        completions.map { all -> all[spaceId].orEmpty().filter { it.completedBy == userId }.take(LATEST_COMPLETIONS) }

    override suspend fun complete(spaceId: String, itemId: String, next: Schedule?) {
        val item = observeItems(spaceId).first().first { it.id == itemId }
        completeItem(spaceId, item, next)
    }

    private suspend fun completeItem(spaceId: String, item: Item, next: Schedule?) {
        if (item.status is ItemStatus.Done && (item.modules.cost != null || item.costExpenseId != null)) return
        val memberIds = if (item.modules.cost != null && item.costExpenseId == null) {
            checkNotNull(spaces).observeMembers(spaceId).first().map { it.userId }
        } else emptyList()
        val expense = itemCostExpense(item, memberIds, Clock.System.todayIn(TimeZone.currentSystemDefault()))
        val expenseId = expense?.let { checkNotNull(expenses).add(spaceId, it) } ?: item.costExpenseId
        val done = ItemStatus.Done(by = auth.requireUser().id, at = Clock.System.now())
        edit(spaceId, item.id) {
            (if (next != null) item.copy(modules = item.modules.with(next)) else item.copy(status = done))
                .copy(
                    completionVersion = item.completionVersion + 1,
                    costExpenseId = if (next != null) null else expenseId
                )
        }
        record(spaceId, item.id, done.by, reopened = false)
    }

    override suspend fun reopen(spaceId: String, itemId: String) {
        edit(spaceId, itemId) { it.copy(status = ItemStatus.Open) }
        record(spaceId, itemId, auth.requireUser().id, reopened = true)
    }

    private fun record(spaceId: String, itemId: String, userId: String, reopened: Boolean) = completions.update { all ->
        val entry =
            Completion("completion-${all.values.sumOf { it.size } + 1}", itemId, userId, Clock.System.now(), reopened)
        all + (spaceId to listOf(entry) + all[spaceId].orEmpty())
    }

    private fun edit(spaceId: String, itemId: String, change: (Item) -> Item) = items.update { all ->
        val space = all[spaceId].orEmpty()
        if (space.none { it.id == itemId }) throw NoSuchElementException("No item $itemId in space $spaceId")
        all + (spaceId to space.map { if (it.id == itemId) change(it) else it })
    }

    private fun editEntry(
        spaceId: String,
        itemId: String,
        entryId: String,
        change: (ChecklistEntry) -> ChecklistEntry
    ) =
        edit(spaceId, itemId) { item ->
            val checklist = checkNotNull(item.modules.checklist) { "Item $itemId has no checklist" }
            check(checklist.entries.any { it.id == entryId }) { "No entry $entryId" }
            val entries = checklist.entries.map { if (it.id == entryId) change(it) else it }
            item.copy(modules = item.modules.with(checklist.copy(entries = entries)))
        }
}
