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

/**
 * The schedule being edited, as typed: nothing reaches the repository until [schedule] is valid.
 * Due on [date] (with an optional [time]) or, as a [window], from [date] to [endDate] with no time.
 */
data class ScheduleEditor(
    val window: Boolean = false,
    val date: LocalDate,
    val endDate: LocalDate = date.plus(DatePeriod(days = 1)),
    /** "HH:MM", or blank for none. Only for a due date. */
    val time: String = "",
    /** Whole minutes, or blank for none. */
    val duration: String = "",
    /** Which date the picker is open for, if any. */
    val picking: ScheduleDate? = null,
) {
    val parsedTime: LocalTime? get() = parseTime(time)
    val timeInvalid: Boolean get() = !window && time.isNotBlank() && parsedTime == null

    private val minutes: Int? get() = duration.trim().toIntOrNull()
    val durationInvalid: Boolean get() = duration.isNotBlank() && (minutes ?: 0) <= 0

    private val candidate: Schedule
        get() = Schedule(
            date = date,
            time = parsedTime.takeIf { !window },
            endDate = endDate.takeIf { window },
            duration = minutes?.takeIf { it > 0 }?.minutes,
        )

    /** The ADR 0002 rules the draft breaks, such as an end not after its start. */
    val errors: List<ItemError> get() = scheduleErrors(candidate)

    /** What Save writes, or null while the draft is invalid. */
    val schedule: Schedule? get() = candidate.takeIf { !timeInvalid && !durationInvalid && errors.isEmpty() }

    /** The date the picker opens on, as the UTC midnight millis M3's date picker takes. */
    val pickerMillis: Long?
        get() = when (picking) {
            ScheduleDate.START -> date
            ScheduleDate.END -> endDate
            null -> null
        }?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()

    companion object {
        fun of(schedule: Schedule) = ScheduleEditor(
            window = schedule.endDate != null,
            date = schedule.date,
            endDate = schedule.endDate ?: schedule.date.plus(DatePeriod(days = 1)),
            time = schedule.time?.let(::formatTime).orEmpty(),
            duration = schedule.duration?.inWholeMinutes?.toString().orEmpty(),
        )
    }
}

enum class ScheduleDate { START, END }

private val TIME = Regex("""(\d{1,2}):(\d{2})""")

private fun parseTime(text: String): LocalTime? {
    val (hour, minute) = TIME.matchEntire(text.trim())?.destructured ?: return null
    return if (hour.toInt() in 0..23 && minute.toInt() in 0..59) LocalTime(hour.toInt(), minute.toInt()) else null
}

private fun formatTime(time: LocalTime) =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
