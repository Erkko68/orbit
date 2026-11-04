package com.orbit.app.feature.item

import com.orbit.app.domain.model.ItemError
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.recurrenceErrors
import com.orbit.app.domain.model.scheduleErrors
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes

private val DefaultPickerTime = LocalTime(9, 0)
private const val MINUTES_PER_HOUR = 60
private const val MONTHS_PER_YEAR = 12

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
    /** How often it repeats, or null when it does not. */
    val repeat: RepeatUnit? = null,
    /** Every how many days, weeks or months: 1 or more. */
    val repeatEvery: Int = 1,
    /** The days of a weekly repeat: never empty, the start's day until others are picked. */
    val repeatWeekdays: Set<DayOfWeek> = setOf(date.dayOfWeek),
    /** The last day it may repeat on, or null to repeat for good. */
    val repeatUntil: LocalDate? = null,
    /** The sheet shows the usual repeats. */
    val editingRepeat: Boolean = false,
    /** The sheet shows the wheels of a repeat of the user's own. */
    val customRepeat: Boolean = false,
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

    /** What Save writes with the schedule. A monthly repeat falls on the start's day of the month. */
    val recurrence: Recurrence?
        get() = when (repeat) {
            RepeatUnit.DAYS -> Recurrence.Days(repeatEvery, repeatUntil)
            RepeatUnit.WEEKS -> Recurrence.Weeks(repeatEvery, repeatWeekdays, repeatUntil)
            RepeatUnit.MONTHS -> Recurrence.Months(repeatEvery, date.day, repeatUntil)
            RepeatUnit.YEARS -> Recurrence.Months(repeatEvery * MONTHS_PER_YEAR, date.day, repeatUntil)
            null -> null
        }

    /** The usual repeats from the start date, not repeating first. Each keeps the last day already picked. */
    val repeatPresets: List<Recurrence?>
        get() = listOf(
            null,
            Recurrence.Days(1, repeatUntil),
            Recurrence.Weeks(1, setOf(date.dayOfWeek), repeatUntil),
            Recurrence.Months(1, date.day, repeatUntil),
            Recurrence.Months(MONTHS_PER_YEAR, date.day, repeatUntil),
        )

    /** It repeats in a way of the user's own, not one of [repeatPresets]. */
    val isCustomRepeat: Boolean get() = recurrence !in repeatPresets

    /** Repeats as [recurrence] says, or not at all. A year is twelve months. */
    fun withRecurrence(recurrence: Recurrence?): ScheduleEditor {
        val years = recurrence is Recurrence.Months && recurrence.every % MONTHS_PER_YEAR == 0
        return copy(
            repeat = when (recurrence) {
                is Recurrence.Days -> RepeatUnit.DAYS
                is Recurrence.Weeks -> RepeatUnit.WEEKS
                is Recurrence.Months -> if (years) RepeatUnit.YEARS else RepeatUnit.MONTHS
                null -> null
            },
            repeatEvery = recurrence?.let { if (years) it.every / MONTHS_PER_YEAR else it.every } ?: 1,
            repeatWeekdays = (recurrence as? Recurrence.Weeks)?.weekdays ?: setOf(date.dayOfWeek),
            repeatUntil = recurrence?.until,
        )
    }

    /** The ADR 0002 rules the draft breaks, such as an end not after its start. */
    val errors: List<ItemError> get() = scheduleErrors(candidate) + recurrenceErrors(recurrence, candidate)

    /** What Save writes, or null while the draft is invalid. */
    val schedule: Schedule? get() = candidate.takeIf { !timeInvalid && errors.isEmpty() }

    /** Picks a weekday of a weekly repeat, or takes it off unless it is the last one. */
    fun withWeekdayToggled(day: DayOfWeek): ScheduleEditor = when {
        day !in repeatWeekdays -> copy(repeatWeekdays = repeatWeekdays + day)
        repeatWeekdays.size > 1 -> copy(repeatWeekdays = repeatWeekdays - day)
        else -> this
    }

    /** The hour and the minute the time picker opens on: the time set, or the top of the morning without one. */
    val pickerTime: LocalTime get() = parsedTime ?: DefaultPickerTime

    private val pickerDate: LocalDate?
        get() = when (picking) {
            ScheduleDate.START -> date
            ScheduleDate.END -> endDate
            ScheduleDate.UNTIL -> repeatUntil ?: date
            null -> null
        }

    /** A date starts today at the earliest, an end after its start, and the last repeat on the start at the earliest. */
    private val pickerEarliest: LocalDate
        get() = when (picking) {
            ScheduleDate.END -> date.nextDay()
            ScheduleDate.UNTIL -> date
            else -> today
        }

    /** The date the picker opens on, as the UTC midnight millis M3's date picker takes. */
    val pickerMillis: Long? get() = pickerDate?.utcMillis()

    /** The calendar's years. A date already in the past stays in them, since M3 refuses to open on a year it does not list. */
    val pickerYears: IntRange get() = minOf(pickerEarliest, pickerDate ?: pickerEarliest).year..LAST_YEAR

    /** A day the calendar lets the user pick, as UTC midnight millis: a valid one, or the one it opened on. */
    fun canPick(utcMillis: Long): Boolean = utcMillis >= pickerEarliest.utcMillis() || utcMillis == pickerMillis

    fun canPickYear(year: Int): Boolean = year >= pickerEarliest.year

    /** Sets the date being picked. A start past the end or the last repeat takes them along, so the draft stays valid. */
    fun withPicked(picked: LocalDate): ScheduleEditor = when (picking) {
        ScheduleDate.START -> copy(
            date = picked,
            endDate = maxOf(endDate, picked.nextDay()),
            repeatUntil = repeatUntil?.let { maxOf(it, picked) },
            picking = null,
        )
        ScheduleDate.END -> copy(endDate = picked, picking = null)
        ScheduleDate.UNTIL -> copy(repeatUntil = picked, picking = null)
        null -> this
    }

    companion object {
        fun of(schedule: Schedule, recurrence: Recurrence?, today: LocalDate): ScheduleEditor {
            val minutes = schedule.duration?.inWholeMinutes?.toInt() ?: 0
            return ScheduleEditor(
                window = schedule.endDate != null,
                date = schedule.date,
                endDate = schedule.endDate ?: schedule.date.nextDay(),
                time = schedule.time?.let(::formatTime).orEmpty(),
                durationHours = minutes / MINUTES_PER_HOUR,
                durationMinutes = minutes % MINUTES_PER_HOUR,
                today = today,
            ).withRecurrence(recurrence)
        }
    }
}

enum class ScheduleDate { START, END, UNTIL }

enum class RepeatUnit { DAYS, WEEKS, MONTHS, YEARS }

private fun LocalDate.nextDay() = plus(DatePeriod(days = 1))

private fun LocalDate.utcMillis() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private val TIME = Regex("""(\d{1,2}):(\d{2})""")

private fun parseTime(text: String): LocalTime? {
    val (hour, minute) = TIME.matchEntire(text.trim())?.destructured ?: return null
    return if (hour.toInt() in 0..23 && minute.toInt() in 0..59) LocalTime(hour.toInt(), minute.toInt()) else null
}

private fun formatTime(time: LocalTime) =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
