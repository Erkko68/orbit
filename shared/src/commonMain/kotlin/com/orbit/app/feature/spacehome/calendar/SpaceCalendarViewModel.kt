package com.orbit.app.feature.spacehome.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.agendaOf
import com.orbit.app.domain.model.covers
import com.orbit.app.domain.model.endOfWeek
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.rolledForward
import com.orbit.app.feature.spacehome.items.toSpaceItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The Calendar destination of a space: the month or the list, and the day picked in the month. */
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceCalendarViewModel(
    private val spaceId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
) : ViewModel() {
    private val start = today()
    private val view = MutableStateFlow(CalendarView.MONTH)
    private val month = MutableStateFlow(start.firstOfMonth())
    private val selected = MutableStateFlow(start)
    private val failed = MutableStateFlow(false)
    private val submitting = MutableStateFlow(false)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    // Members only decorate the rows: a slow or failed read must not hide the items.
    private val members = flow { emitAll(spaces.observeMembers(spaceId)) }
        .onStart { emit(emptyList()) }
        .orOnFailure("Loading assignees for the calendar of $spaceId", emptyList())

    // One query covers both the agenda's week and the month shown.
    private val loaded = month
        .flatMapLatest { first -> items.observeAgenda(spaceId, maxOf(endOfWeek(start), first.lastOfMonth())) }
        .asLoad("Loading the calendar of $spaceId", retries)

    private val picks = combine(view, month, selected, failed, submitting) { view, month, selected, failed, submitting ->
        SpaceCalendarUiState(view = view, month = month, selected = selected, failed = failed, submitting = submitting)
    }

    val uiState: StateFlow<SpaceCalendarUiState> = combine(loaded, picks, members) { load, picks, members ->
        val open = load.valueOrNull.orEmpty().filter { it.status is ItemStatus.Open }
        val today = today()
        val agenda = agendaOf(open, today)
        val first = requireNotNull(picks.month)
        val last = first.lastOfMonth()
        fun rows(list: List<Item>) = list.map { toSpaceItem(it, members) }
        picks.copy(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            today = today,
            marked = open.flatMap { it.modules.schedule?.daysBetween(first, last).orEmpty() }.toSet(),
            selectedItems = rows(
                open.filter { it.modules.schedule?.covers(requireNotNull(picks.selected)) == true }
                    .sortedWith(compareBy({ it.modules.schedule?.date }, { it.modules.schedule?.time })),
            ),
            overdue = rows(agenda.overdue),
            days = agenda.days.map { CalendarDay(it.date, rows(it.items)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceCalendarUiState(loading = true))

    fun selectView(view: CalendarView) {
        this.view.value = view
    }

    fun selectDay(day: LocalDate) {
        selected.value = day
    }

    fun previousMonth() = showMonth(month.value.minus(1, DateTimeUnit.MONTH))

    fun nextMonth() = showMonth(month.value.plus(1, DateTimeUnit.MONTH))

    /** Only open items are shown, so a row's check only completes. A recurring item moves to its next date (ADR 0011). */
    fun complete(itemId: String) {
        if (submitting.value) return
        submitting.value = true
        failed.value = false
        viewModelScope.launch {
            try {
                val result = attempt("Completing item $itemId in space $spaceId") {
                    val item = items.observeItems(spaceId).first().firstOrNull { it.id == itemId }
                    items.complete(spaceId, itemId, item?.modules?.rolledForward(today()))
                }
                failed.value = result.isFailure
            } finally {
                submitting.value = false
            }
        }
    }

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Picks today in its own month, the 1st in any other. */
    private fun showMonth(first: LocalDate) {
        month.value = first
        val today = today()
        selected.value = if (today.firstOfMonth() == first) today else first
    }
}

private fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

private fun LocalDate.firstOfMonth() = LocalDate(year, month, 1)

private fun LocalDate.lastOfMonth() = firstOfMonth().plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

/** The days the schedule covers between [first] and [last]. */
private fun Schedule.daysBetween(first: LocalDate, last: LocalDate): List<LocalDate> {
    val from = maxOf(date, first)
    val to = minOf(deadline, last)
    return if (from > to) emptyList() else List(from.daysUntil(to) + 1) { from.plus(it, DateTimeUnit.DAY) }
}
