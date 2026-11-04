package com.orbit.app.feature.item

import com.orbit.app.domain.model.ItemError
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class ScheduleEditorTest {
    private val day = LocalDate(2026, 10, 26)

    @Test
    fun opensOnWhatTheScheduleHolds() {
        val editor = ScheduleEditor.of(Schedule(day, time = LocalTime(9, 5), duration = 90.minutes), recurrence = null, today = day)

        assertFalse(editor.window)
        assertEquals("09:05", editor.time)
        assertEquals(1, editor.durationHours)
        assertEquals(30, editor.durationMinutes)
        assertEquals(LocalDate(2026, 10, 27), editor.endDate)
        assertEquals(Schedule(day, time = LocalTime(9, 5), duration = 90.minutes), editor.schedule)
    }

    @Test
    fun opensAWindowAsOne() {
        val editor = ScheduleEditor.of(Schedule(day, endDate = LocalDate(2026, 10, 30)), recurrence = null, today = day)

        assertTrue(editor.window)
        assertEquals(Schedule(day, endDate = LocalDate(2026, 10, 30)), editor.schedule)
    }

    @Test
    fun aBadTimeIsNotSaved() {
        for (time in listOf("9", "24:00", "12:60", "noon")) {
            val editor = ScheduleEditor(date = day, time = time)
            assertTrue(editor.timeInvalid, time)
            assertNull(editor.schedule, time)
        }
        assertEquals(LocalTime(7, 30), ScheduleEditor(date = day, time = " 7:30 ").schedule?.time)
    }

    @Test
    fun theWheelsAddUpToTheLengthAndNothingOnThemIsNoLength() {
        assertNull(ScheduleEditor(date = day).schedule?.duration)
        assertEquals(90.minutes, ScheduleEditor(date = day, durationHours = 1, durationMinutes = 30).schedule?.duration)
        assertEquals(6039.minutes, ScheduleEditor(date = day, durationHours = 99, durationMinutes = 99).schedule?.duration)
    }

    @Test
    fun aWindowDropsTheTimeAndNeedsItsEndAfterItsStart() {
        val window = ScheduleEditor(window = true, date = day, time = "09:00")
        assertEquals(Schedule(day, endDate = LocalDate(2026, 10, 27)), window.schedule)

        val backwards = window.copy(endDate = day)
        assertEquals(listOf<ItemError>(ItemError.EndNotAfterStart), backwards.errors)
        assertNull(backwards.schedule)
    }

    @Test
    fun givesThePickerTheDateItEdits() {
        val editor = ScheduleEditor(date = day, endDate = LocalDate(2026, 10, 27))

        assertNull(editor.pickerMillis)
        assertEquals(1_792_972_800_000, editor.copy(picking = ScheduleDate.START).pickerMillis)
        assertEquals(1_793_059_200_000, editor.copy(picking = ScheduleDate.END).pickerMillis)
    }

    private fun millis(date: LocalDate) = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    @Test
    fun theCalendarStartsToday() {
        val editor = ScheduleEditor(date = day.plus(DatePeriod(days = 3)), today = day, picking = ScheduleDate.START)

        assertFalse(editor.canPick(millis(LocalDate(2026, 10, 25))))
        assertTrue(editor.canPick(millis(day)))
        assertTrue(editor.canPick(millis(LocalDate(2027, 1, 1))))
        assertFalse(editor.canPickYear(2025))
        assertTrue(editor.canPickYear(2026))
    }

    @Test
    fun anEndComesAfterItsStart() {
        val editor = ScheduleEditor(window = true, date = day, today = day, picking = ScheduleDate.END)

        assertFalse(editor.canPick(millis(day)))
        assertTrue(editor.canPick(millis(LocalDate(2026, 10, 27))))
    }

    @Test
    fun aDateInThePastStaysAsItWasButNoOtherPastDayCanBePicked() {
        val overdue = LocalDate(2025, 12, 20)
        val editor = ScheduleEditor(date = overdue, today = day, picking = ScheduleDate.START)

        assertTrue(editor.canPick(millis(overdue)))
        assertFalse(editor.canPick(millis(LocalDate(2025, 12, 21))))
        assertEquals(2025, editor.pickerYears.first)
    }

    @Test
    fun aStartPastTheEndTakesTheEndAlong() {
        val window = ScheduleEditor(window = true, date = day, today = day, picking = ScheduleDate.START)

        val moved = window.withPicked(LocalDate(2026, 11, 5))

        assertEquals(LocalDate(2026, 11, 5), moved.date)
        assertEquals(LocalDate(2026, 11, 6), moved.endDate)
        assertNull(moved.picking)
        assertEquals(emptyList<ItemError>(), moved.errors)
    }

    @Test
    fun aRepeatFollowsWhatIsPicked() {
        // 26 October 2026 is a Monday.
        val editor = ScheduleEditor(date = day)

        assertNull(editor.recurrence)
        assertEquals(Recurrence.Days(1), editor.copy(repeat = RepeatUnit.DAYS).recurrence)
        assertEquals(Recurrence.Weeks(2, setOf(DayOfWeek.MONDAY)), editor.copy(repeat = RepeatUnit.WEEKS, repeatEvery = 2).recurrence)
        assertEquals(Recurrence.Months(1, 26), editor.copy(repeat = RepeatUnit.MONTHS).recurrence)
    }

    @Test
    fun aWeeklyRepeatKeepsItsLastDay() {
        val editor = ScheduleEditor(date = day, repeat = RepeatUnit.WEEKS)

        val both = editor.withWeekdayToggled(DayOfWeek.FRIDAY)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), both.repeatWeekdays)
        assertEquals(setOf(DayOfWeek.FRIDAY), both.withWeekdayToggled(DayOfWeek.MONDAY).repeatWeekdays)
        assertEquals(editor, editor.withWeekdayToggled(DayOfWeek.MONDAY))
    }

    @Test
    fun theLastRepeatIsPickedFromTheStartAndMovesWithIt() {
        val later = LocalDate(2026, 11, 20)
        val editor = ScheduleEditor(date = day, repeat = RepeatUnit.DAYS, picking = ScheduleDate.UNTIL)

        assertFalse(editor.canPick(LocalDate(2026, 10, 25).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()))
        val until = editor.withPicked(LocalDate(2026, 11, 2))
        assertEquals(Recurrence.Days(1, LocalDate(2026, 11, 2)), until.recurrence)

        val moved = until.copy(picking = ScheduleDate.START).withPicked(later)
        assertEquals(later, moved.repeatUntil)
        assertEquals(Schedule(later), moved.schedule)
    }

    @Test
    fun opensOnTheRepeatItHas() {
        val editor = ScheduleEditor.of(Schedule(day), Recurrence.Weeks(3, setOf(DayOfWeek.SUNDAY), day), today = day)

        assertEquals(RepeatUnit.WEEKS, editor.repeat)
        assertEquals(Recurrence.Weeks(3, setOf(DayOfWeek.SUNDAY), day), editor.recurrence)
    }
}
