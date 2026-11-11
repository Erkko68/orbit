package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/**
 * A calendar for a page of an [OrbitSheet], with the button that confirms the day: [onPick] gets it as UTC midnight
 * millis. The picker's own state holds the day being browsed; the caller only hears the one confirmed. [canPick] and
 * [canPickYear] leave days out, and [years] are the ones it lists.
 */
// DatePicker is experimental on iOS only (stable on Android): an exception AGENTS.md allows.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitDatePicker(
    selectedMillis: Long?,
    pickText: String,
    onPick: (Long) -> Unit,
    years: IntRange = DatePickerDefaults.YearRange,
    canPick: (Long) -> Boolean = { true },
    canPickYear: (Int) -> Boolean = { true },
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        yearRange = years,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = canPick(utcTimeMillis)

            override fun isSelectableYear(year: Int) = canPickYear(year)
        },
    )
    val scheme = MaterialTheme.colorScheme
    DatePicker(
        state = state,
        modifier = Modifier.spanSheetGutter(),
        title = null,
        headline = null,
        showModeToggle = false,
        colors = DatePickerDefaults.colors(
            // The sheet's own colour: the list of years is painted with it, over the days.
            containerColor = scheme.surfaceBright,
            selectedDayContainerColor = OrbitTheme.accent.vivid,
            selectedDayContentColor = Ink,
            selectedYearContainerColor = OrbitTheme.accent.vivid,
            selectedYearContentColor = Ink,
            todayContentColor = scheme.onSurface,
            todayDateBorderColor = scheme.onSurface,
        ),
    )
    OrbitButton(
        text = pickText,
        onClick = { state.selectedDateMillis?.let(onPick) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.selectedDateMillis != null,
    )
}

/**
 * M3's picker is laid out for 360dp, more than a sheet leaves inside its gutter on a 375pt iPhone: there the last
 * column, Sunday, came out narrower than the rest. It spans the sheet's gutter instead, which its own inner padding
 * still keeps off the screen's edges (#293).
 */
private fun Modifier.spanSheetGutter() = layout { measurable, constraints ->
    val gutter = OrbitSpacing.md.roundToPx()
    val width = constraints.maxWidth + 2 * gutter
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-gutter, 0) }
}
