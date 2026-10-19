package com.orbit.app.feature.spacehome.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.feature.item.icon
import com.orbit.app.feature.item.label
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The items tab of a space: its items and the filter over them. */
class SpaceItemsViewModel(spaceId: String, items: ItemRepository) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    val uiState: StateFlow<SpaceItemsUiState> = combine(
        items.observeItems(spaceId).asLoad("Loading the items of $spaceId", retries),
        filter,
    ) { load, selected ->
        SpaceItemsUiState(
            filter = selected,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            items = load.valueOrNull.orEmpty().filter { selected.matches(it.preset) }.map(::toSpaceItem),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceItemsUiState(loading = true))

    fun selectFilter(filter: ItemFilter) {
        this.filter.value = filter
    }

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }
}

private fun toSpaceItem(item: Item) = SpaceItem(item.id, item.title, item.preset.label, item.preset.icon)
