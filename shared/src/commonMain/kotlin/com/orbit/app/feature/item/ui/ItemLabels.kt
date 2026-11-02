package com.orbit.app.feature.item.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.orbit.app.core.formatDate
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_hours
import orbit.shared.generated.resources.duration_hours_minutes
import orbit.shared.generated.resources.duration_minutes
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** Uses the platform's regional date format, including the selected per-app locale. */
@Composable
internal fun itemDateLabel(date: LocalDate): String = formatDate(date, Locale.current.toLanguageTag())

/** A duration in hours and minutes ("1 h 30 min"), worded by the resources. */
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
