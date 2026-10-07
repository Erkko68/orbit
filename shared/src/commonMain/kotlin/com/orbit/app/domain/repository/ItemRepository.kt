package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import kotlinx.coroutines.flow.Flow

/**
 * Narrow operations only, no `save(item)`: each write touches one field or one module so that
 * concurrent edits merge (ADR 0002). Callers check `validateItem` first. Acts as the signed-in
 * user. Writes suspend until done and throw on failure.
 */
interface ItemRepository {
    /** Items of a space, oldest first. */
    fun observeItems(spaceId: String): Flow<List<Item>>

    /** Returns the new item's id. */
    suspend fun create(spaceId: String, item: NewItem): String

    suspend fun rename(spaceId: String, itemId: String, title: String)

    /** Adds the module, or replaces the one of the same type. */
    suspend fun setModule(spaceId: String, itemId: String, module: Module)

    suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType)

    suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean)

    suspend fun complete(spaceId: String, itemId: String)

    suspend fun reopen(spaceId: String, itemId: String)
}
