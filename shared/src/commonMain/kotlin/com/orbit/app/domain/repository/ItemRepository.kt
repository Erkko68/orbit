package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Narrow operations only, no `save(item)`: each write touches one field or one module so that
 * concurrent edits merge (ADR 0002). Callers check `validateItem` first. Acts as the signed-in
 * user. Writes return once applied locally, without waiting for the server, so they work offline.
 * They throw on failure.
 */
interface ItemRepository {
    /** Items of a space, oldest first. */
    fun observeItems(spaceId: String): Flow<List<Item>>

    /** Open items of a space dated up to [until], overdue ones included, unsorted: `agendaOf` sorts them. */
    fun observeAgenda(spaceId: String, until: LocalDate): Flow<List<Item>>

    /** Returns the new item's id. */
    suspend fun create(spaceId: String, item: NewItem): String

    /** Only its creator, an admin or the owner may. */
    suspend fun delete(spaceId: String, itemId: String)

    suspend fun rename(spaceId: String, itemId: String, title: String)

    /** Null takes the description off. It also clears the retired `details`, which reads as part of it (ADR 0010). */
    suspend fun setDescription(spaceId: String, itemId: String, description: String?)

    /** Adds the module, or replaces the one of the same type. */
    suspend fun setModule(spaceId: String, itemId: String, module: Module)

    suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType)

    suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean)

    suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int)

    suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?)

    suspend fun removeEntry(spaceId: String, itemId: String, entryId: String)

    /** The latest times the item was completed or reopened, newest first. */
    fun observeCompletions(spaceId: String, itemId: String): Flow<List<Completion>>

    /** The latest items [userId] completed or reopened in the space, newest first. */
    fun observeMemberCompletions(spaceId: String, userId: String): Flow<List<Completion>>

    /**
     * Also records the completion in the item's history, which reopening keeps. With a [next] schedule the item
     * recurs: it stays open and moves there instead (ADR 0011).
     * A cost adds its expense in the same write, once per occurrence: completing again after reopening adds none
     * (ADR 0014).
     */
    suspend fun complete(spaceId: String, itemId: String, next: Schedule?)

    /** Also records the reopening in the item's history. */
    suspend fun reopen(spaceId: String, itemId: String)
}
