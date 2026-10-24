package com.orbit.app.feature.spacehome

import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.validateItem
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_tab_agenda
import orbit.shared.generated.resources.space_tab_items
import org.jetbrains.compose.resources.StringResource

/** The header of a space, its selected tab and the "New item" dialog. The member row and items have their own state. */
data class SpaceHomeUiState(
    val name: String = "",
    val memberCount: Int = 0,
    /** The signed-in user, for the account avatar in the top bar. */
    val userInitials: String = "",
    val userPhotoUrl: String? = null,
    val tab: SpaceHomeTab = SpaceHomeTab.ITEMS,
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
    /** The item just created reminds, so notifications get allowed before it opens. */
    val askNotifications: Boolean = false,
) {
    /** The item to open now: the one just created, once notifications are settled. */
    val itemToOpen: String? get() = createdItemId.takeIf { !askNotifications }
    val canCreate: Boolean get() = !creating && validateItem(newItemTitle, ItemModules(), emptyList()).isEmpty()
}

enum class SpaceHomeTab(val label: StringResource) {
    ITEMS(Res.string.space_tab_items),
    AGENDA(Res.string.space_tab_agenda),
}

