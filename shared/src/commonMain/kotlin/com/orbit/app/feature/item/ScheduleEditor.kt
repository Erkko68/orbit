package com.orbit.app.feature.item

import com.orbit.app.domain.model.ItemError
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.scheduleErrors
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes

private val DefaultPickerTime = LocalTime(9, 0)
private const val MINUTES_PER_HOUR = 60

/** Where the calendar's list of years ends, as M3's default does. */
private const val LAST_YEAR = 2100

/**
 * The schedule being edited, as typed: nothing reaches the repository until [schedule] is valid.
 * Due on [date] (with an optional [time]) or, as a [window], from [date] to [endDate] with no time.
 */
data class ScheduleEditor(
    val window: Boolean = false,
    val date: LocalDate,
    val endDate: LocalDate = date.nextDay(),
    /** "HH:MM", or blank for none. Only for a due date. */
    val time: String = "",
    /** How long it takes, on the two wheels. Nothing on either means no length. */
    val durationHours: Int = 0,
    val durationMinutes: Int = 0,
    /** The first day the calendar offers. */
    val today: LocalDate = date,
    /** Which date the sheet is picking, if any. */
    val picking: ScheduleDate? = null,
    /** The sheet is picking the time. */
    val pickingTime: Boolean = false,
) {
    val parsedTime: LocalTime? get() = parseTime(time)
    val timeInvalid: Boolean get() = !window && time.isNotBlank() && parsedTime == null

    private val minutes: Int get() = durationHours * MINUTES_PER_HOUR + durationMinutes

    private val candidate: Schedule
        get() = Schedule(
            date = date,
            time = parsedTime.takeIf { !window },
            endDate = endDate.takeIf { window },
            duration = minutes.takeIf { it > 0 }?.minutes,
        )

    /** The ADR 0002 rules the draft breaks, such as an end not after its start. */
    val errors: List<ItemError> get() = scheduleErrors(candidate)

    /** What Save writes, or null while the draft is invalid. */
    val schedule: Schedule? get() = candidate.takeIf { !timeInvalid && errors.isEmpty() }

    /** The hour and the minute the time picker opens on: the time set, or the top of the morning without one. */
    val pickerTime: LocalTime get() = parsedTime ?: DefaultPickerTime

    private val pickerDate: LocalDate?
        get() = when (picking) {
            ScheduleDate.START -> date
            ScheduleDate.END -> endDate
            null -> null
        }

    /** A date starts today at the earliest, and an end after its start. */
    private val pickerEarliest: LocalDate get() = if (picking == ScheduleDate.END) date.nextDay() else today

    /** The date the picker opens on, as the UTC midnight millis M3's date picker takes. */
    val pickerMillis: Long? get() = pickerDate?.utcMillis()

    /** The calendar's years. A date already in the past stays in them, since M3 refuses to open on a year it does not list. */
    val pickerYears: IntRange get() = minOf(pickerEarliest, pickerDate ?: pickerEarliest).year..LAST_YEAR

    /** A day the calendar lets the user pick, as UTC midnight millis: a valid one, or the one it opened on. */
    fun canPick(utcMillis: Long): Boolean = utcMillis >= pickerEarliest.utcMillis() || utcMillis == pickerMillis

    fun canPickYear(year: Int): Boolean = year >= pickerEarliest.year

    /** Sets the date being picked. A start past the end takes the end along, so a window stays one. */
    fun withPicked(picked: LocalDate): ScheduleEditor = when (picking) {
        ScheduleDate.START -> copy(date = picked, endDate = maxOf(endDate, picked.nextDay()), picking = null)
        ScheduleDate.END -> copy(endDate = picked, picking = null)
        null -> this
    }

    companion object {
        fun of(schedule: Schedule, today: LocalDate): ScheduleEditor {
            val minutes = schedule.duration?.inWholeMinutes?.toInt() ?: 0
            return ScheduleEditor(
                window = schedule.endDate != null,
                date = schedule.date,
                endDate = schedule.endDate ?: schedule.date.nextDay(),
                time = schedule.time?.let(::formatTime).orEmpty(),
                durationHours = minutes / MINUTES_PER_HOUR,
                durationMinutes = minutes % MINUTES_PER_HOUR,
                today = today,
            )
        }
    }
}

enum class ScheduleDate { START, END }

private fun LocalDate.nextDay() = plus(DatePeriod(days = 1))

private fun LocalDate.utcMillis() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private val TIME = Regex("""(\d{1,2}):(\d{2})""")

private fun parseTime(text: String): LocalTime? {
    val (hour, minute) = TIME.matchEntire(text.trim())?.destructured ?: return null
    return if (hour.toInt() in 0..23 && minute.toInt() in 0..59) LocalTime(hour.toInt(), minute.toInt()) else null
}

private fun formatTime(time: LocalTime) =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
