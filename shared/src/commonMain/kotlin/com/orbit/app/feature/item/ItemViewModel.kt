package com.orbit.app.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ItemViewModel(
    private val spaceId: String,
    private val itemId: String,
    private val items: ItemRepository,
    private val spaces: SpaceRepository,
) : ViewModel() {
    private val actionState = MutableStateFlow(ActionState())
    private val retries = Channel<Unit>(Channel.CONFLATED)

    // Context is secondary: a failed space/member read must not hide an otherwise readable item.
    private val context = flow {
        emitAll(combine(spaces.observeSpaces(), spaces.observeMembers(spaceId)) { all, members ->
            ItemContext(
                spaceName = all.firstOrNull { it.id == spaceId }?.name,
                memberNames = members.associate { it.userId to it.displayName },
            )
        })
    }.catch { cause ->
        Logger.e(cause) { "Loading context for item $itemId failed" }
        emit(ItemContext(failed = true))
    }.onStart { emit(ItemContext()) }

    // Wrap the call too, so a failure when opening the stream can be retried.
    private val item = flow { emitAll(items.observeItems(spaceId)) }
        .map<List<Item>, ItemLoad> { all -> ItemLoad.Loaded(all.firstOrNull { it.id == itemId }) }
        .retryWhen { cause, _ ->
            Logger.e(cause) { "Loading item $itemId in space $spaceId failed" }
            emit(ItemLoad.Failed)
            retries.receive()
            emit(ItemLoad.Loading)
            true
        }
        .onStart { emit(ItemLoad.Loading) }

    val uiState: StateFlow<ItemUiState> = combine(
        item,
        actionState,
        context,
    ) { load, action, details ->
        val loaded = (load as? ItemLoad.Loaded)?.item
        ItemUiState(
            itemId = itemId,
            spaceId = spaceId,
            title = loaded?.title.orEmpty(),
            description = loaded?.description,
            preset = loaded?.preset,
            status = loaded?.status ?: ItemStatus.Open,
            modules = loaded?.modules?.all.orEmpty(),
            loading = load == ItemLoad.Loading,
            loadFailed = load == ItemLoad.Failed,
            notFound = load is ItemLoad.Loaded && loaded == null,
            failed = action.failed,
            mutating = action.mutating,
            spaceName = details.spaceName,
            creatorName = loaded?.let { details.memberNames[it.createdBy] },
            createdDate = loaded?.createdAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date,
            memberNames = details.memberNames,
            detailsFailed = details.failed,
            today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemUiState(itemId = itemId, spaceId = spaceId))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun addModule(type: ModuleType) {
        if (type !in uiState.value.availableModules) return
        val defaultModule: Module = when (type) {
            ModuleType.SCHEDULE -> {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                Schedule(date = today)
            }

            ModuleType.ASSIGNMENT -> Assignment.Anyone
            ModuleType.CHECKLIST -> Checklist(entries = emptyList())
            ModuleType.REMINDER -> Reminder(offsets = listOf(Duration.ZERO))
        }
        mutate { items.setModule(spaceId, itemId, defaultModule) }
    }

    fun removeModule(type: ModuleType) {
        val current = uiState.value
        if (current.modules.none { it.type == type }) return
        val hasReminder = current.modules.any { it.type == ModuleType.REMINDER }
        mutate {
            if (type == ModuleType.SCHEDULE && hasReminder) {
                items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            }
            items.removeModule(spaceId, itemId, type)
        }
    }

    fun toggleStatus() {
        val currentStatus = uiState.value.status
        mutate {
            if (currentStatus is ItemStatus.Open) {
                items.complete(spaceId, itemId)
            } else {
                items.reopen(spaceId, itemId)
            }
        }
    }

    private fun mutate(action: suspend () -> Unit) {
        if (!uiState.value.canMutate || actionState.value.mutating) return
        actionState.update { it.copy(mutating = true, failed = false) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Item operation failed on item $itemId in space $spaceId" }
                actionState.update { it.copy(failed = true) }
            } finally {
                actionState.update { it.copy(mutating = false) }
            }
        }
    }

    private data class ActionState(val mutating: Boolean = false, val failed: Boolean = false)
    private data class ItemContext(
        val spaceName: String? = null,
        val memberNames: Map<String, String> = emptyMap(),
        val failed: Boolean = false,
    )
}

private sealed interface ItemLoad {
    data object Loading : ItemLoad
    data object Failed : ItemLoad
    data class Loaded(val item: Item?) : ItemLoad
}
