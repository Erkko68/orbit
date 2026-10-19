package com.orbit.app.feature.item.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_hours
import orbit.shared.generated.resources.duration_hours_minutes
import orbit.shared.generated.resources.duration_minutes
import orbit.shared.generated.resources.item_date_format
import orbit.shared.generated.resources.item_month_1
import orbit.shared.generated.resources.item_month_10
import orbit.shared.generated.resources.item_month_11
import orbit.shared.generated.resources.item_month_12
import orbit.shared.generated.resources.item_month_2
import orbit.shared.generated.resources.item_month_3
import orbit.shared.generated.resources.item_month_4
import orbit.shared.generated.resources.item_month_5
import orbit.shared.generated.resources.item_month_6
import orbit.shared.generated.resources.item_month_7
import orbit.shared.generated.resources.item_month_8
import orbit.shared.generated.resources.item_month_9
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** Month names and date order come from resources, so translations can follow the device language. */
@Composable
internal fun itemDateLabel(date: LocalDate): String {
    val month = when (date.month) {
        Month.JANUARY -> Res.string.item_month_1
        Month.FEBRUARY -> Res.string.item_month_2
        Month.MARCH -> Res.string.item_month_3
        Month.APRIL -> Res.string.item_month_4
        Month.MAY -> Res.string.item_month_5
        Month.JUNE -> Res.string.item_month_6
        Month.JULY -> Res.string.item_month_7
        Month.AUGUST -> Res.string.item_month_8
        Month.SEPTEMBER -> Res.string.item_month_9
        Month.OCTOBER -> Res.string.item_month_10
        Month.NOVEMBER -> Res.string.item_month_11
        Month.DECEMBER -> Res.string.item_month_12
    }
    return stringResource(Res.string.item_date_format, stringResource(month), date.day, date.year)
}

/** A duration in hours and minutes ("1 h 30 min"), worded by the resources like the dates. */
@Composable
internal fun itemDurationLabel(duration: Duration): String {
    val hours = duration.inWholeHours.toInt()
    val minutes = (duration.inWholeMinutes % 60).toInt()
    return when {
        hours == 0 -> stringResource(Res.string.duration_minutes, minutes)
        minutes == 0 -> stringResource(Res.string.duration_hours, hours)
        else -> stringResource(Res.string.duration_hours_minutes, hours, minutes)
    }
}
