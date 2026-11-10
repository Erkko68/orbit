package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.firstDayOfWeek
import com.orbit.app.core.formatMonth
import com.orbit.app.core.weekdayInitials
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_left
import orbit.shared.generated.resources.ic_chevron_right

private const val DAYS_IN_WEEK = 7
private val TodayRing = 2.dp
private val DayGap = 2.dp

/**
 * A month on a panel, laid out like the date picker: its name with the buttons to the month before and after, then its
 * weeks, starting on the day the phone's region starts them. [marked] days are filled, the [selected] one is ink and
 * today has a ring. [month] is any day of the month to show. [markedLabel] tells a screen reader that a day is marked.
 */
@Composable
fun OrbitMonth(
    month: LocalDate,
    today: LocalDate?,
    selected: LocalDate?,
    marked: Set<LocalDate>,
    markedLabel: String,
    onSelect: (LocalDate) -> Unit,
    previousLabel: String,
    onPrevious: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = Locale.current.toLanguageTag()
    val firstDay = firstDayOfWeek(language)
    val first = LocalDate(month.year, month.month, 1)
    // Blank cells before the 1st, so it falls under its weekday.
    val lead = (first.dayOfWeek.isoDayNumber - firstDay.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val length = first.daysUntil(first.plus(1, DateTimeUnit.MONTH))
    val cells = List(lead) { null } + List(length) { first.plus(it, DateTimeUnit.DAY) }
    val initials = weekdayInitials(language)
    val weekdays = List(DAYS_IN_WEEK) { initials[(firstDay.isoDayNumber - 1 + it) % DAYS_IN_WEEK] }
    val scheme = MaterialTheme.colorScheme

    OrbitCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMonth(first, language), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            OrbitIconButton(Res.drawable.ic_chevron_left, previousLabel, onPrevious)
            OrbitIconButton(Res.drawable.ic_chevron_right, nextLabel, onNext)
        }
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            Row {
                for (weekday in weekdays) {
                    Text(
                        weekday,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            for (week in cells.chunked(DAYS_IN_WEEK)) {
                Row {
                    for (index in 0 until DAYS_IN_WEEK) {
                        val day = week.getOrNull(index)
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(DayGap), Alignment.Center) {
                            if (day != null) {
                                Day(day, day == today, day == selected, day in marked, markedLabel) { onSelect(day) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(day: LocalDate, today: Boolean, selected: Boolean, marked: Boolean, markedLabel: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (fill, ink) = when {
        selected -> scheme.primary to scheme.onPrimary
        marked -> OrbitTheme.accent.vivid to Ink
        else -> Color.Transparent to scheme.onSurface
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(fill)
            .then(if (today && !selected) Modifier.border(TodayRing, scheme.onSurface, CircleShape) else Modifier)
            .selectable(selected = selected, onClick = onClick)
            .semantics { if (marked) stateDescription = markedLabel },
        Alignment.Center,
    ) {
        Text(day.day.toString(), style = MaterialTheme.typography.titleSmall, color = ink)
    }
}
