package com.orbit.app.feature.item

import com.orbit.app.domain.model.ItemError
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
        val editor = ScheduleEditor.of(Schedule(day, time = LocalTime(9, 5), duration = 90.minutes))

        assertFalse(editor.window)
        assertEquals("09:05", editor.time)
        assertEquals("90", editor.duration)
        assertEquals(LocalDate(2026, 10, 27), editor.endDate)
        assertEquals(Schedule(day, time = LocalTime(9, 5), duration = 90.minutes), editor.schedule)
    }

    @Test
    fun opensAWindowAsOne() {
        val editor = ScheduleEditor.of(Schedule(day, endDate = LocalDate(2026, 10, 30)))

        assertTrue(editor.window)
        assertEquals(Schedule(day, endDate = LocalDate(2026, 10, 30)), editor.schedule)
    }

    @Test
    fun aBadTimeOrDurationIsNotSaved() {
        for (time in listOf("9", "24:00", "12:60", "noon")) {
            val editor = ScheduleEditor(date = day, time = time)
            assertTrue(editor.timeInvalid, time)
            assertNull(editor.schedule, time)
        }
        for (duration in listOf("0", "-5", "abc")) {
            val editor = ScheduleEditor(date = day, duration = duration)
            assertTrue(editor.durationInvalid, duration)
            assertNull(editor.schedule, duration)
        }
        assertEquals(LocalTime(7, 30), ScheduleEditor(date = day, time = " 7:30 ").schedule?.time)
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
}
