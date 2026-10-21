package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/** Items in memory, with ids in creation order across spaces. */
class InMemoryItemRepository(private val auth: AuthRepository) : ItemRepository {
    /** The items of each space, by its id. */
    private val items = MutableStateFlow(emptyMap<String, List<Item>>())
    private var created = 0
    private var createdEntries = 0

    override fun observeItems(spaceId: String): Flow<List<Item>> = items.map { it[spaceId].orEmpty() }

    override suspend fun create(spaceId: String, item: NewItem): String {
        val id = "item-${++created}"
        val stored = Item(
            id = id,
            title = item.title,
            description = item.description,
            status = ItemStatus.Open,
            preset = item.preset,
            createdBy = auth.requireUser().id,
            createdAt = Clock.System.now(),
            modules = item.modules,
        )
        items.update { it + (spaceId to it[spaceId].orEmpty() + stored) }
        return id
    }

    override suspend fun rename(spaceId: String, itemId: String, title: String) =
        edit(spaceId, itemId) { it.copy(title = title) }

    override suspend fun setModule(spaceId: String, itemId: String, module: Module) =
        edit(spaceId, itemId) { it.copy(modules = it.modules.with(module)) }

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) =
        edit(spaceId, itemId) { it.copy(modules = it.modules.without(type)) }

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) =
        editEntry(spaceId, itemId, entryId) { it.copy(checked = checked) }

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) =
        edit(spaceId, itemId) { item ->
            val checklist = checkNotNull(item.modules.checklist) { "Item $itemId has no checklist" }
            while (checklist.entries.any { it.id == "entry-${createdEntries + 1}" }) createdEntries++
            val entry = ChecklistEntry("entry-${++createdEntries}", text, quantity = quantity, order = order)
            item.copy(modules = item.modules.with(checklist.copy(entries = checklist.entries + entry)))
        }

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) =
        editEntry(spaceId, itemId, entryId) { it.copy(text = text, quantity = quantity) }

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) =
        edit(spaceId, itemId) { item ->
            val checklist = checkNotNull(item.modules.checklist) { "Item $itemId has no checklist" }
            check(checklist.entries.any { it.id == entryId }) { "No entry $entryId" }
            item.copy(modules = item.modules.with(checklist.copy(entries = checklist.entries.filterNot { it.id == entryId })))
        }

    override suspend fun complete(spaceId: String, itemId: String) {
        val done = ItemStatus.Done(by = auth.requireUser().id, at = Clock.System.now())
        edit(spaceId, itemId) { it.copy(status = done) }
    }

    override suspend fun reopen(spaceId: String, itemId: String) =
        edit(spaceId, itemId) { it.copy(status = ItemStatus.Open) }

    private fun edit(spaceId: String, itemId: String, change: (Item) -> Item) = items.update { all ->
        val space = all[spaceId].orEmpty()
        if (space.none { it.id == itemId }) throw NoSuchElementException("No item $itemId in space $spaceId")
        all + (spaceId to space.map { if (it.id == itemId) change(it) else it })
    }

    private fun editEntry(spaceId: String, itemId: String, entryId: String, change: (ChecklistEntry) -> ChecklistEntry) =
        edit(spaceId, itemId) { item ->
            val checklist = checkNotNull(item.modules.checklist) { "Item $itemId has no checklist" }
            check(checklist.entries.any { it.id == entryId }) { "No entry $entryId" }
            val entries = checklist.entries.map { if (it.id == entryId) change(it) else it }
            item.copy(modules = item.modules.with(checklist.copy(entries = entries)))
        }
}
