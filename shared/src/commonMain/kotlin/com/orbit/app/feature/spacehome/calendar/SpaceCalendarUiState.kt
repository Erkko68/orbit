package com.orbit.app.feature.spacehome.calendar

import com.orbit.app.feature.spacehome.items.SpaceItem
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.calendar_view_list
import orbit.shared.generated.resources.calendar_view_month
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** The Calendar destination: a month with the items of the day picked under it, or the agenda as a list by day. */
data class SpaceCalendarUiState(
    val view: CalendarView = CalendarView.MONTH,
    /** The items have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the items failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val today: LocalDate? = null,
    /** The first day of the month shown. */
    val month: LocalDate? = null,
    val selected: LocalDate? = null,
    /** The days of [month] that have open items. */
    val marked: Set<LocalDate> = emptySet(),
    /** The open items on [selected]. */
    val selectedItems: List<SpaceItem> = emptyList(),
    /** The list: what is overdue, then each day to the end of the week that has something. */
    val overdue: List<SpaceItem> = emptyList(),
    val days: List<CalendarDay> = emptyList(),
    /** Completing an item failed. Cleared by the next attempt. */
    val failed: Boolean = false,
    /** A complete write is in progress. */
    val submitting: Boolean = false,
) {
    val agendaEmpty: Boolean get() = overdue.isEmpty() && days.isEmpty()
}

data class CalendarDay(val date: LocalDate, val items: List<SpaceItem>)

/** How Calendar shows the items. */
enum class CalendarView(val label: StringResource, val icon: DrawableResource) {
    MONTH(Res.string.calendar_view_month, Res.drawable.ic_event),
    LIST(Res.string.calendar_view_list, Res.drawable.ic_list),
}
