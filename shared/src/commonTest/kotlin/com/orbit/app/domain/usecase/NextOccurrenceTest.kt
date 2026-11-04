package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.THURSDAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class NextOccurrenceTest {

    private fun next(date: LocalDate, recurrence: Recurrence, after: LocalDate = date) =
        nextOccurrence(Schedule(date), recurrence, after)?.date

    @Test
    fun everyNDaysCountsFromTheSchedulesDate() {
        val monday = LocalDate(2026, 11, 2)

        assertEquals(LocalDate(2026, 11, 5), next(monday, Recurrence.Days(3)))
        // Completed late: the next one keeps the rhythm, after the day it was done.
        assertEquals(LocalDate(2026, 11, 11), next(monday, Recurrence.Days(3), after = LocalDate(2026, 11, 9)))
        // Before the schedule's date, the first occurrence is that date.
        assertEquals(monday, next(monday, Recurrence.Days(3), after = LocalDate(2026, 10, 1)))
    }

    @Test
    fun weeklyRulesCrossTheWeekBoundary() {
        val friday = LocalDate(2026, 11, 6)
        val mondayAndFriday = Recurrence.Weeks(1, setOf(MONDAY, FRIDAY))

        assertEquals(LocalDate(2026, 11, 9), next(friday, mondayAndFriday))
        assertEquals(LocalDate(2026, 11, 13), next(LocalDate(2026, 11, 9), mondayAndFriday))
        // Across a year too.
        assertEquals(LocalDate(2027, 1, 1), next(LocalDate(2026, 12, 28), mondayAndFriday))
    }

    @Test
    fun everyOtherWeekSkipsTheWeekBetween() {
        val thursday = LocalDate(2026, 11, 5)
        val rule = Recurrence.Weeks(2, setOf(MONDAY, THURSDAY))

        assertEquals(LocalDate(2026, 11, 16), next(thursday, rule))
        assertEquals(LocalDate(2026, 11, 19), next(LocalDate(2026, 11, 16), rule))
        // A late completion lands on the next week of the rhythm, not the following week.
        assertEquals(LocalDate(2026, 11, 30), next(thursday, rule, after = LocalDate(2026, 11, 20)))
    }

    @Test
    fun aWeeklyRuleStartsOnAChosenDayAfterAnOffDate() {
        val wednesday = LocalDate(2026, 11, 4)

        assertEquals(LocalDate(2026, 11, 7), next(wednesday, Recurrence.Weeks(1, setOf(SATURDAY)), after = LocalDate(2026, 11, 3)))
    }

    @Test
    fun theThirtyFirstFallsOnTheLastDayOfShorterMonths() {
        val rule = Recurrence.Months(1, 31)

        assertEquals(LocalDate(2026, 2, 28), next(LocalDate(2026, 1, 31), rule))
        assertEquals(LocalDate(2026, 3, 31), next(LocalDate(2026, 2, 28), rule))
        assertEquals(LocalDate(2026, 4, 30), next(LocalDate(2026, 3, 31), rule))
    }

    @Test
    fun leapYearsKeepTheTwentyNinth() {
        assertEquals(LocalDate(2028, 2, 29), next(LocalDate(2028, 1, 31), Recurrence.Months(1, 31)))
        assertEquals(LocalDate(2028, 2, 29), next(LocalDate(2027, 2, 28), Recurrence.Months(12, 29)))
        assertEquals(LocalDate(2029, 2, 28), next(LocalDate(2028, 2, 29), Recurrence.Months(12, 29)))
    }

    @Test
    fun everyNMonthsCountsFromTheSchedulesMonth() {
        val rule = Recurrence.Months(3, 15)

        assertEquals(LocalDate(2027, 1, 15), next(LocalDate(2026, 10, 15), rule))
        // Completed much later: the next quarter's 15th after that day.
        assertEquals(LocalDate(2027, 7, 15), next(LocalDate(2026, 10, 15), rule, after = LocalDate(2027, 4, 20)))
        // A start after the day of the month waits for the next month of the rhythm.
        assertEquals(LocalDate(2027, 1, 15), next(LocalDate(2026, 10, 20), rule, after = LocalDate(2026, 10, 19)))
    }

    @Test
    fun theEndDateIsTheLastPossibleOccurrence() {
        val monday = LocalDate(2026, 11, 2)

        assertEquals(LocalDate(2026, 11, 9), next(monday, Recurrence.Days(7, until = LocalDate(2026, 11, 9))))
        assertNull(next(monday, Recurrence.Days(7, until = LocalDate(2026, 11, 8))))
        assertNull(next(monday, Recurrence.Months(1, 2, until = LocalDate(2026, 12, 1))))
    }

    @Test
    fun aWindowMovesWithItsStartAndKeepsTimeAndDuration() {
        val window = Schedule(LocalDate(2026, 11, 6), endDate = LocalDate(2026, 11, 8))
        val timed = Schedule(LocalDate(2026, 11, 6), time = LocalTime(20, 0), duration = 30.minutes)

        assertEquals(
            Schedule(LocalDate(2026, 11, 13), endDate = LocalDate(2026, 11, 15)),
            nextOccurrence(window, Recurrence.Weeks(1, setOf(FRIDAY)), after = window.date),
        )
        assertEquals(
            timed.copy(date = LocalDate(2026, 11, 7)),
            nextOccurrence(timed, Recurrence.Days(1), after = timed.date),
        )
    }

    @Test
    fun aRuleThatNeverRepeatsHasNoNextOccurrence() {
        val friday = LocalDate(2026, 11, 6)

        assertNull(next(friday, Recurrence.Weeks(1, emptySet())))
        assertFailsWith<IllegalArgumentException> { next(friday, Recurrence.Days(0)) }
    }

    @Test
    fun completingOnTimeOrLateMovesPastBothTheDateAndToday() {
        val friday = LocalDate(2026, 11, 6)
        val weekly = ItemModules(schedule = Schedule(friday), recurrence = Recurrence.Weeks(1, setOf(FRIDAY)))

        assertEquals(Schedule(LocalDate(2026, 11, 13)), weekly.rolledForward(today = LocalDate(2026, 11, 4)))
        assertEquals(Schedule(LocalDate(2026, 11, 20)), weekly.rolledForward(today = LocalDate(2026, 11, 14)))
        assertNull(ItemModules(schedule = Schedule(friday)).rolledForward(friday))
        assertNull(weekly.copy(recurrence = Recurrence.Days(1, until = friday)).rolledForward(friday))
    }
}
