package com.orbit.app.feature.spacehome.items

import com.orbit.app.domain.model.Preset
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.filter_all
import orbit.shared.generated.resources.filter_bills
import orbit.shared.generated.resources.filter_chores
import orbit.shared.generated.resources.filter_events
import orbit.shared.generated.resources.filter_lists
import orbit.shared.generated.resources.filter_tasks
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class SpaceItemsUiState(
    val filter: ItemFilter = ItemFilter.ALL,
    /** The space's items that pass [filter]. */
    val items: List<SpaceItem> = emptyList(),
)

data class SpaceItem(val id: String, val title: String, val type: ItemType)

enum class ItemType(val label: StringResource, val icon: DrawableResource) {
    TASK(Res.string.item_type_task, Res.drawable.ic_task),
    LIST(Res.string.item_type_list, Res.drawable.ic_list),
    EVENT(Res.string.item_type_event, Res.drawable.ic_event),
    OTHER(Res.string.item_type_other, Res.drawable.ic_task),
}

/** The chips over the items tab. Chores and bills match nothing until their presets land (Sprint 2). */
enum class ItemFilter(val label: StringResource, val matches: (Preset?) -> Boolean) {
    ALL(Res.string.filter_all, { true }),
    TASKS(Res.string.filter_tasks, { it == Preset.TASK }),
    CHORES(Res.string.filter_chores, { false }),
    BILLS(Res.string.filter_bills, { false }),
    LISTS(Res.string.filter_lists, { it == Preset.SHOPPING_LIST }),
    EVENTS(Res.string.filter_events, { it == Preset.EVENT }),
}
