package com.orbit.app.feature.spacehome

import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_calendar_selected
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.ic_task_selected
import orbit.shared.generated.resources.ic_wallet
import orbit.shared.generated.resources.ic_wallet_selected
import orbit.shared.generated.resources.space_tab_calendar
import orbit.shared.generated.resources.space_tab_expenses
import orbit.shared.generated.resources.space_tab_tasks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The header of a space and its selected destination. The member row and items have their own state. */
data class SpaceHomeUiState(
    val name: String = "",
    /** The space's colour, null until it has loaded. */
    val accent: Int? = null,
    /** The signed-in user, for the account avatar in the top bar. */
    val userInitials: String = "",
    val userPhotoUrl: String? = null,
    val tab: SpaceHomeTab = SpaceHomeTab.CALENDAR,
    val today: LocalDate? = null,
    /** What Calendar opens with. Null until the items load, or if they could not. */
    val headline: SpaceHeadline? = null,
) {
    /** Expenses will add expenses instead (#54). */
    val canAddItem: Boolean get() = tab != SpaceHomeTab.EXPENSES
}

/** What Calendar says first, so a member knows where they stand without reading the agenda. */
sealed interface SpaceHeadline {
    /** The space has no items at all. */
    data object Empty : SpaceHeadline

    /** Nothing is due today for the user. */
    data object AllClear : SpaceHeadline

    data class Due(val count: Int) : SpaceHeadline
}

/** The destinations of a space's bottom bar (ADR 0005). */
enum class SpaceHomeTab(val label: StringResource, val icon: DrawableResource, val selectedIcon: DrawableResource) {
    CALENDAR(Res.string.space_tab_calendar, Res.drawable.ic_event, Res.drawable.ic_calendar_selected),
    TASKS(Res.string.space_tab_tasks, Res.drawable.ic_task, Res.drawable.ic_task_selected),
    EXPENSES(Res.string.space_tab_expenses, Res.drawable.ic_wallet, Res.drawable.ic_wallet_selected),
}
