package com.orbit.app.testing

import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** An [ItemRepository] whose writes fail with [failure]. Optionally emits [items]. */
internal class FailingItemRepository(
    private val failure: Exception = IllegalStateException("Write failed"),
    private val items: List<Item> = emptyList(),
) : ItemRepository {
    override fun observeItems(spaceId: String): Flow<List<Item>> = flowOf(items)

    override suspend fun create(spaceId: String, item: NewItem): String = throw failure

    override suspend fun rename(spaceId: String, itemId: String, title: String) = throw failure

    override suspend fun setModule(spaceId: String, itemId: String, module: Module) = throw failure

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) = throw failure

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) =
        throw failure

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) = throw failure

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) =
        throw failure

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) = throw failure

    override suspend fun complete(spaceId: String, itemId: String) = throw failure

    override suspend fun reopen(spaceId: String, itemId: String) = throw failure
}
