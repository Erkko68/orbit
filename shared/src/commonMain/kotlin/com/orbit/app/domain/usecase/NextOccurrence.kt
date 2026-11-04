package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus

/**
 * [schedule] moved to the first occurrence of [recurrence] after [after], or null when that falls after the
 * recurrence's `until`. Occurrences start at the schedule's date, and the time, the duration and the length of
 * a window come along (ADR 0011).
 */
fun nextOccurrence(schedule: Schedule, recurrence: Recurrence, after: LocalDate): Schedule? {
    require(recurrence.every >= 1) { "A recurrence repeats every 1 or more" }
    val start = maxOf(after.plus(1, DateTimeUnit.DAY), schedule.date)
    val next = when (recurrence) {
        is Recurrence.Days -> nextDay(schedule.date, recurrence.every, start)
        is Recurrence.Weeks -> nextWeekday(schedule.date, recurrence, start)
        is Recurrence.Months -> nextMonthDay(schedule.date, recurrence, start)
    } ?: return null
    if (recurrence.until?.let { next > it } == true) return null
    val shift = schedule.date.daysUntil(next)
    return schedule.copy(date = next, endDate = schedule.endDate?.plus(shift, DateTimeUnit.DAY))
}

private fun nextDay(anchor: LocalDate, every: Int, start: LocalDate): LocalDate {
    val steps = (anchor.daysUntil(start) + every - 1) / every
    return anchor.plus(steps * every, DateTimeUnit.DAY)
}

/** The next [Recurrence.Weeks.every]th week holds a chosen day within that many weeks; null without weekdays. */
private fun nextWeekday(anchor: LocalDate, rule: Recurrence.Weeks, start: LocalDate): LocalDate? {
    val anchorWeek = anchor.monday()
    return (0 until 7 * rule.every).asSequence()
        .map { start.plus(it, DateTimeUnit.DAY) }
        .firstOrNull { day ->
            day.dayOfWeek in rule.weekdays && anchorWeek.daysUntil(day.monday()) / 7 % rule.every == 0
        }
}

private fun nextMonthDay(anchor: LocalDate, rule: Recurrence.Months, start: LocalDate): LocalDate {
    val anchorMonth = LocalDate(anchor.year, anchor.month, 1)
    val firstStep = anchorMonth.monthsUntil(start) / rule.every
    return generateSequence(firstStep) { it + 1 }
        .map { step -> anchorMonth.plus(step * rule.every, DateTimeUnit.MONTH).onDay(rule.dayOfMonth) }
        .first { it >= start }
}

private fun LocalDate.monday(): LocalDate = minus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

/** [day] of this date's month, or its last day when the month is shorter. */
private fun LocalDate.onDay(day: Int): LocalDate {
    val last = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    return LocalDate(year, month, minOf(day, last.day))
}
