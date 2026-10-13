package com.orbit.app.feature.spacehome.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The items tab of a space: its items and the filter over them. */
class SpaceItemsViewModel(spaceId: String, items: ItemRepository) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)

    val uiState: StateFlow<SpaceItemsUiState> = combine(items.observeItems(spaceId), filter) { spaceItems, selected ->
        SpaceItemsUiState(
            filter = selected,
            items = spaceItems.filter { selected.matches(it.preset) }.map(::toSpaceItem),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceItemsUiState())

    fun selectFilter(filter: ItemFilter) {
        this.filter.value = filter
    }
}

private fun toSpaceItem(item: Item) = SpaceItem(
    id = item.id,
    title = item.title,
    type = when (item.preset) {
        Preset.TASK -> ItemType.TASK
        Preset.SHOPPING_LIST -> ItemType.LIST
        Preset.EVENT -> ItemType.EVENT
        null -> ItemType.OTHER
    },
)
