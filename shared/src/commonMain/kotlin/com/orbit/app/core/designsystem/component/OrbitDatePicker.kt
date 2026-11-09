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
import com.orbit.app.core.designsystem.theme.Ink
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
