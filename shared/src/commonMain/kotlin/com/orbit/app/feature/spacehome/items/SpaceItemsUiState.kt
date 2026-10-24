package com.orbit.app.feature.spacehome.items

import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.Preset
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.filter_all
import orbit.shared.generated.resources.filter_events
import orbit.shared.generated.resources.filter_lists
import orbit.shared.generated.resources.filter_tasks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class SpaceItemsUiState(
    val filter: ItemFilter = ItemFilter.ALL,
    /** The items have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the items failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    /** The space's items that pass [filter]. */
    val items: List<SpaceItem> = emptyList(),
    /** Completing or reopening an item failed. Cleared by the next attempt. */
    val failed: Boolean = false,
    /** A complete or reopen write is currently in progress. */
    val submitting: Boolean = false,
) {
    /** For the count next to the section header. */
    val openCount: Int get() = items.count { !it.done }
}

data class SpaceItem(
    val id: String,
    val title: String,
    val label: StringResource,
    val icon: DrawableResource,
    val done: Boolean = false,
    val assignmentLabel: StringResource? = null,
    val assigneeName: String? = null,
    val dueDate: LocalDate? = null,
    val checklist: SpaceItemChecklist? = null,
) {
    val assigneeInitials: String? get() = assigneeName?.let(::initialsOf)
    val hasFooter: Boolean get() = assignmentLabel != null || dueDate != null
}

data class SpaceItemChecklist(val done: Int, val total: Int) {
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** The chips over the items tab, one per preset. Chores and bills join when their presets do (#52, #59). */
enum class ItemFilter(val label: StringResource, val matches: (Preset?) -> Boolean) {
    ALL(Res.string.filter_all, { true }),
    TASKS(Res.string.filter_tasks, { it == Preset.TASK }),
    LISTS(Res.string.filter_lists, { it == Preset.SHOPPING_LIST }),
    EVENTS(Res.string.filter_events, { it == Preset.EVENT }),
}
