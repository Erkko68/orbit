package com.orbit.app.testing

import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Real in-memory behaviour with observable calls and controllable delays/failures for tests. */
internal class ControlledItemRepository(private val delegate: ItemRepository) : ItemRepository by delegate {
    val writes = mutableListOf<String>()
    var readAttempts = 0
        private set
    var beforeRead: suspend () -> Unit = {}
    var beforeWrite: suspend (String) -> Unit = {}

    override fun observeItems(spaceId: String): Flow<List<Item>> = flow {
        readAttempts++
        beforeRead()
        emitAll(delegate.observeItems(spaceId))
    }

    override suspend fun setModule(spaceId: String, itemId: String, module: Module) {
        write("set:${module.type}") { delegate.setModule(spaceId, itemId, module) }
    }

    override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) {
        write("remove:$type") { delegate.removeModule(spaceId, itemId, type) }
    }

    override suspend fun addEntry(spaceId: String, itemId: String, text: String, quantity: Int?, order: Int) {
        write("addEntry") { delegate.addEntry(spaceId, itemId, text, quantity, order) }
    }

    override suspend fun updateEntry(spaceId: String, itemId: String, entryId: String, text: String, quantity: Int?) {
        write("updateEntry") { delegate.updateEntry(spaceId, itemId, entryId, text, quantity) }
    }

    override suspend fun removeEntry(spaceId: String, itemId: String, entryId: String) {
        write("removeEntry") { delegate.removeEntry(spaceId, itemId, entryId) }
    }

    override suspend fun setEntryChecked(spaceId: String, itemId: String, entryId: String, checked: Boolean) {
        write("checkEntry") { delegate.setEntryChecked(spaceId, itemId, entryId, checked) }
    }

    override suspend fun complete(spaceId: String, itemId: String) {
        write("complete") { delegate.complete(spaceId, itemId) }
    }

    override suspend fun reopen(spaceId: String, itemId: String) {
        write("reopen") { delegate.reopen(spaceId, itemId) }
    }

    private suspend fun write(operation: String, action: suspend () -> Unit) {
        writes += operation
        beforeWrite(operation)
        action()
    }
}
