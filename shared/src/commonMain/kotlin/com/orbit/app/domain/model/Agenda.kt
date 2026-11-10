package com.orbit.app.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** The open items dated up to the end of the week: what is overdue, then one day after another. */
data class Agenda(
    val overdue: List<Item> = emptyList(),
    /** Only the days that have something, today first. */
    val days: List<AgendaDay> = emptyList(),
)

data class AgendaDay(val date: LocalDate, val items: List<Item>)

/** The last day the agenda shows: the Sunday of [today]'s week, weeks starting on Monday. */
fun endOfWeek(today: LocalDate): LocalDate =
    today.plus(DayOfWeek.SUNDAY.isoDayNumber - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)

/** The schedule runs over [day]: from its date to its deadline. */
fun Schedule.covers(day: LocalDate): Boolean = date <= day && day <= deadline

/**
 * Sorts [items] into the agenda of [today]: overdue once their deadline has passed, today while
 * today is within their date and deadline, and on the day they start later in the week. Dates are
 * wall-clock (ADR 0002), so the phone's time zone matters only for what [today] is. Each day runs
 * in the order the items start, all-day before timed. Done, undated and later items are left out.
 */
fun agendaOf(items: List<Item>, today: LocalDate): Agenda {
    val end = endOfWeek(today)
    val scheduled = items
        .filter { it.status is ItemStatus.Open }
        .mapNotNull { item -> item.modules.schedule?.let { item to it } }
        .sortedWith(compareBy({ it.second.date }, { it.second.time }))

    return Agenda(
        overdue = scheduled.filter { it.second.deadline < today }.map { it.first },
        days = scheduled
            .filter { it.second.deadline >= today && it.second.date <= end }
            .groupBy({ maxOf(it.second.date, today) }, { it.first })
            .entries.sortedBy { it.key }
            .map { AgendaDay(it.key, it.value) },
    )
}
