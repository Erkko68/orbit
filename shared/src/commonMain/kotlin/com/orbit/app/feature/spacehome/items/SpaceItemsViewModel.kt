package com.orbit.app.feature.spacehome.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.feature.item.icon
import com.orbit.app.feature.item.label
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The items tab of a space: its items and the filter over them. */
class SpaceItemsViewModel(private val spaceId: String, private val items: ItemRepository) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)
    private val actionFailed = MutableStateFlow(false)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    val uiState: StateFlow<SpaceItemsUiState> = combine(
        items.observeItems(spaceId).asLoad("Loading the items of $spaceId", retries),
        filter,
        actionFailed,
    ) { load, selected, failed ->
        val filtered = load.valueOrNull.orEmpty().filter { selected.matches(it.preset) }
        val (open, done) = filtered.partition { it.status is ItemStatus.Open }
        SpaceItemsUiState(
            filter = selected,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            items = (open + done).map(::toSpaceItem),
            failed = failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceItemsUiState(loading = true))

    fun selectFilter(filter: ItemFilter) {
        this.filter.value = filter
    }

    fun complete(itemId: String) {
        viewModelScope.launch {
            actionFailed.value = false
            val result = attempt("Completing item $itemId in space $spaceId") {
                items.complete(spaceId, itemId)
            }
            actionFailed.value = result.isFailure
        }
    }

    fun reopen(itemId: String) {
        viewModelScope.launch {
            actionFailed.value = false
            val result = attempt("Reopening item $itemId in space $spaceId") {
                items.reopen(spaceId, itemId)
            }
            actionFailed.value = result.isFailure
        }
    }

    fun toggleDone(itemId: String, done: Boolean) {
        if (done) complete(itemId) else reopen(itemId)
    }

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }
}

private fun toSpaceItem(item: Item) = SpaceItem(
    id = item.id,
    title = item.title,
    label = item.preset.label,
    icon = item.preset.icon,
    done = item.status is ItemStatus.Done,
)
