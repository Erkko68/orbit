package com.orbit.app.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** The open items dated up to the end of the week, by when they fall. */
data class Agenda(
    val overdue: List<Item> = emptyList(),
    val today: List<Item> = emptyList(),
    val thisWeek: List<Item> = emptyList(),
)

/** The last day the agenda shows: the Sunday of [today]'s week, weeks starting on Monday. */
fun endOfWeek(today: LocalDate): LocalDate =
    today.plus(DayOfWeek.SUNDAY.isoDayNumber - today.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)

/**
 * Sorts [items] into the agenda of [today]: overdue once their deadline has passed, today while
 * today is within their date and deadline, this week when they start later in the week. Dates are
 * wall-clock (ADR 0002), so the phone's time zone matters only for what [today] is. Each section runs
 * in the order the items start, all-day before timed. Done, undated and later items are left out.
 */
fun agendaOf(items: List<Item>, today: LocalDate): Agenda {
    val end = endOfWeek(today)
    val scheduled = items
        .filter { it.status is ItemStatus.Open }
        .mapNotNull { item -> item.modules.schedule?.let { item to it } }
        .sortedWith(compareBy({ it.second.date }, { it.second.time }))

    fun section(fallsIn: (Schedule) -> Boolean) = scheduled.filter { fallsIn(it.second) }.map { it.first }

    return Agenda(
        overdue = section { it.deadline < today },
        today = section { it.date <= today && today <= it.deadline },
        thisWeek = section { today < it.date && it.date <= end },
    )
}
