package com.orbit.app.feature.spacehome

import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.validateItem
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.ic_wallet
import orbit.shared.generated.resources.space_tab_agenda
import orbit.shared.generated.resources.space_tab_expenses
import orbit.shared.generated.resources.space_tab_home
import orbit.shared.generated.resources.space_tab_items
import orbit.shared.generated.resources.space_tab_tasks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The header of a space, its selected destination and the "New item" dialog. The member row and items have their own state. */
data class SpaceHomeUiState(
    val name: String = "",
    val memberCount: Int = 0,
    /** The signed-in user, for the account avatar in the top bar. */
    val userInitials: String = "",
    val userPhotoUrl: String? = null,
    /** Opens on the items until Home has more than the members (#241). */
    val tab: SpaceHomeTab = SpaceHomeTab.TASKS,
    val tasksView: SpaceTasksView = SpaceTasksView.ITEMS,
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
    /** Expenses will add expenses instead (#54). */
    val canAddItem: Boolean get() = tab != SpaceHomeTab.EXPENSES
    val canCreate: Boolean get() = !creating && validateItem(newItemTitle, ItemModules(), emptyList()).isEmpty()
}

/** The destinations of a space's bottom bar (ADR 0005). */
enum class SpaceHomeTab(val label: StringResource, val icon: DrawableResource) {
    HOME(Res.string.space_tab_home, Res.drawable.ic_home),
    TASKS(Res.string.space_tab_tasks, Res.drawable.ic_task),
    EXPENSES(Res.string.space_tab_expenses, Res.drawable.ic_wallet),
}

/** The two views of the Tasks destination. */
enum class SpaceTasksView(val label: StringResource) {
    ITEMS(Res.string.space_tab_items),
    AGENDA(Res.string.space_tab_agenda),
}

