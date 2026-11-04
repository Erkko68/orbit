package com.orbit.app.feature.item.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.orbit.app.core.formatDate
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_days
import orbit.shared.generated.resources.duration_hours
import orbit.shared.generated.resources.duration_hours_minutes
import orbit.shared.generated.resources.duration_minutes
import orbit.shared.generated.resources.item_due_today
import orbit.shared.generated.resources.item_due_tomorrow
import orbit.shared.generated.resources.item_due_yesterday
import orbit.shared.generated.resources.item_reminders_count
import orbit.shared.generated.resources.module_reminder_at_time
import orbit.shared.generated.resources.module_reminder_before
import orbit.shared.generated.resources.module_schedule_range
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.repeat_every_days
import orbit.shared.generated.resources.repeat_every_months
import orbit.shared.generated.resources.repeat_every_weeks
import orbit.shared.generated.resources.repeat_every_years
import orbit.shared.generated.resources.repeat_month_day
import orbit.shared.generated.resources.weekday_fri
import orbit.shared.generated.resources.weekday_mon
import orbit.shared.generated.resources.weekday_sat
import orbit.shared.generated.resources.weekday_sun
import orbit.shared.generated.resources.weekday_thu
import orbit.shared.generated.resources.weekday_tue
import orbit.shared.generated.resources.weekday_wed
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** Uses the platform's regional date format, including the selected per-app locale. */
@Composable
internal fun itemDateLabel(date: LocalDate): String = formatDate(date, Locale.current.toLanguageTag())

/** When a schedule falls, on one line: its dates, and the time when it has one. */
@Composable
internal fun itemScheduleLabel(schedule: Schedule): String {
    val date = schedule.endDate?.let { end ->
        stringResource(Res.string.module_schedule_range, itemDateLabel(schedule.date), itemDateLabel(end))
    } ?: itemDateLabel(schedule.date)
    return schedule.time?.let { time ->
        stringResource(Res.string.module_schedule_time, date, time.toString())
    } ?: date
}

/** When a recurring item falls, on one line: its next date and how it repeats. */
@Composable
internal fun itemWhenLabel(schedule: Schedule, recurrence: Recurrence?): String =
    recurrence?.let { stringResource(Res.string.module_schedule_time, itemScheduleLabel(schedule), itemRecurrenceLabel(it)) }
        ?: itemScheduleLabel(schedule)

/** How often it repeats ("Every 2 weeks"), with the days of a weekly repeat or the day of a monthly one. Twelve months are a year. */
@Composable
internal fun itemRecurrenceLabel(recurrence: Recurrence): String {
    val every = recurrence.every
    return when (recurrence) {
        is Recurrence.Days -> pluralStringResource(Res.plurals.repeat_every_days, every, every)
        is Recurrence.Weeks -> stringResource(
            Res.string.module_schedule_time,
            pluralStringResource(Res.plurals.repeat_every_weeks, every, every),
            recurrence.weekdays.sorted().map { itemWeekdayLabel(it) }.joinToString(", "),
        )
        is Recurrence.Months -> if (every % 12 == 0) {
            pluralStringResource(Res.plurals.repeat_every_years, every / 12, every / 12)
        } else {
            stringResource(
                Res.string.module_schedule_time,
                pluralStringResource(Res.plurals.repeat_every_months, every, every),
                stringResource(Res.string.repeat_month_day, recurrence.dayOfMonth),
            )
        }
    }
}

/** A day of the week, short. */
@Composable
internal fun itemWeekdayLabel(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> Res.string.weekday_mon
        DayOfWeek.TUESDAY -> Res.string.weekday_tue
        DayOfWeek.WEDNESDAY -> Res.string.weekday_wed
        DayOfWeek.THURSDAY -> Res.string.weekday_thu
        DayOfWeek.FRIDAY -> Res.string.weekday_fri
        DayOfWeek.SATURDAY -> Res.string.weekday_sat
        DayOfWeek.SUNDAY -> Res.string.weekday_sun
    },
)

/** One moment of a reminder. */
@Composable
internal fun itemReminderLabel(offset: Duration): String =
    if (offset == Duration.ZERO) {
        stringResource(Res.string.module_reminder_at_time)
    } else {
        stringResource(Res.string.module_reminder_before, itemDurationLabel(offset))
    }

/** What the header says about a reminder: when, how many moments it has, or nothing without one, where the bell is crossed out. */
@Composable
internal fun itemReminderSummary(reminder: Reminder?): String? {
    val offsets = reminder?.offsets.orEmpty()
    return when (offsets.size) {
        0 -> null
        1 -> itemReminderLabel(offsets.single())
        else -> stringResource(Res.string.item_reminders_count, offsets.size)
    }
}

/** When something is due, in words for the days around [today] and as a date for the rest. */
@Composable
internal fun itemDueLabel(date: LocalDate, today: LocalDate?): String = when (today?.let { date.toEpochDays() - it.toEpochDays() }) {
    -1L -> stringResource(Res.string.item_due_yesterday)
    0L -> stringResource(Res.string.item_due_today)
    1L -> stringResource(Res.string.item_due_tomorrow)
    else -> itemDateLabel(date)
}

/** A duration in whole days, or in hours and minutes ("1 h 30 min"), worded by the resources like the dates. */
@Composable
internal fun itemDurationLabel(duration: Duration): String {
    if (duration >= 1.days && duration == duration.inWholeDays.days) {
        val days = duration.inWholeDays.toInt()
        return pluralStringResource(Res.plurals.duration_days, days, days)
    }
    val hours = duration.inWholeHours.toInt()
    val minutes = (duration.inWholeMinutes % 60).toInt()
    return when {
        hours == 0 -> stringResource(Res.string.duration_minutes, minutes)
        minutes == 0 -> stringResource(Res.string.duration_hours, hours)
        else -> stringResource(Res.string.duration_hours_minutes, hours, minutes)
    }
}
