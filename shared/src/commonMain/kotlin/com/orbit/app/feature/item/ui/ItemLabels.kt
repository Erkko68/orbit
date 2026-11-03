package com.orbit.app.feature.item.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.orbit.app.core.formatDate
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
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
