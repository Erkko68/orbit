package com.orbit.app.feature.spacehome

import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_tab_agenda
import orbit.shared.generated.resources.space_tab_items
import orbit.shared.generated.resources.space_tab_members
import org.jetbrains.compose.resources.StringResource

/** The header of a space and its selected tab. The items and members tabs have their own state. */
data class SpaceHomeUiState(
    val name: String = "",
    /** One entry per member, for the row of avatars. */
    val memberInitials: List<String> = emptyList(),
    val tab: SpaceHomeTab = SpaceHomeTab.ITEMS,
    val openTasks: Int = 0,
    // Placeholders: nothing counts these until bills and the agenda land.
    val billsDue: Int = 0,
    val eventsToday: Int = 0,
)

enum class SpaceHomeTab(val label: StringResource) {
    ITEMS(Res.string.space_tab_items),
    AGENDA(Res.string.space_tab_agenda),
    MEMBERS(Res.string.space_tab_members),
}
