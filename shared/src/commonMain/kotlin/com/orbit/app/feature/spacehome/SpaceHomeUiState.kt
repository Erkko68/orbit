package com.orbit.app.feature.spacehome

import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.validateItem
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_tab_agenda
import orbit.shared.generated.resources.space_tab_items
import orbit.shared.generated.resources.space_tab_members
import org.jetbrains.compose.resources.StringResource

/** The header of a space, its selected tab and the "New item" dialog. The items and members tabs have their own state. */
data class SpaceHomeUiState(
    val name: String = "",
    /** One entry per member, for the row of avatars. */
    val memberInitials: List<String> = emptyList(),
    val tab: SpaceHomeTab = SpaceHomeTab.ITEMS,
    val openTasks: Int = 0,
    // Placeholders: nothing counts these until bills and the agenda land.
    val billsDue: Int = 0,
    val eventsToday: Int = 0,
    /** The "New item" dialog is showing. */
    val newItemOpen: Boolean = false,
    /** What the dialog creates: the preset picked and the name typed. */
    val newItemPreset: Preset = Preset.TASK,
    val newItemTitle: String = "",
    val creating: Boolean = false,
    /** The last create failed. Cleared by the next edit or attempt. */
    val createFailed: Boolean = false,
    /** The item just created, for the screen to open once. */
    val createdItemId: String? = null,
) {
    val canCreate: Boolean get() = !creating && validateItem(newItemTitle, ItemModules(), emptyList()).isEmpty()
}

enum class SpaceHomeTab(val label: StringResource) {
    ITEMS(Res.string.space_tab_items),
    AGENDA(Res.string.space_tab_agenda),
    MEMBERS(Res.string.space_tab_members),
}

