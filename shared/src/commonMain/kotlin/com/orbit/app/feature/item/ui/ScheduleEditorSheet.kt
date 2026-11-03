package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitShapeBadge
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitSheetPages
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTimeWheel
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.ItemError
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.ScheduleDate
import com.orbit.app.feature.item.ScheduleEditor
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.duration_unit_hours
import orbit.shared.generated.resources.duration_unit_minutes
import orbit.shared.generated.resources.ic_clock
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.module_schedule_title
import orbit.shared.generated.resources.schedule_add_time
import orbit.shared.generated.resources.schedule_due
import orbit.shared.generated.resources.schedule_end_not_after_start
import orbit.shared.generated.resources.schedule_from
import orbit.shared.generated.resources.schedule_how_long
import orbit.shared.generated.resources.schedule_no_time
import orbit.shared.generated.resources.schedule_on
import orbit.shared.generated.resources.schedule_pick
import orbit.shared.generated.resources.schedule_pick_date
import orbit.shared.generated.resources.schedule_pick_time
import orbit.shared.generated.resources.schedule_remove
import orbit.shared.generated.resources.schedule_save
import orbit.shared.generated.resources.schedule_tap_to_change
import orbit.shared.generated.resources.schedule_tap_to_set
import orbit.shared.generated.resources.schedule_to
import orbit.shared.generated.resources.schedule_window
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DURATION = "duration"

private enum class ScheduleStep { FORM, DATE, TIME }

private class ScheduleRow(val title: String, val icon: DrawableResource, val shape: OrbitShape, val set: Boolean, val onClick: () -> Unit)

/**
 * Edits an item's schedule in one sheet that changes in place: a due date with an optional time, or a window of dates,
 * and how long it takes. The rows slide to M3's calendar or clock and come back with what was picked.
 */
@Composable
internal fun ScheduleEditorSheet(editor: ScheduleEditor, state: ItemUiState, viewModel: ItemViewModel) {
    val step = when {
        editor.picking != null -> ScheduleStep.DATE
        editor.pickingTime -> ScheduleStep.TIME
        else -> ScheduleStep.FORM
    }
    OrbitSheet(
        title = stringResource(
            when (step) {
                ScheduleStep.FORM -> Res.string.module_schedule_title
                ScheduleStep.DATE -> Res.string.schedule_pick_date
                ScheduleStep.TIME -> Res.string.schedule_pick_time
            },
        ),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissScheduleEditor,
        backDescription = stringResource(Res.string.back),
        onBack = when (step) {
            ScheduleStep.FORM -> null
            ScheduleStep.DATE -> viewModel::dismissScheduleDatePicker
            ScheduleStep.TIME -> viewModel::dismissScheduleTimePicker
        },
    ) {
        OrbitSheetPages(step) { shown ->
            when (shown) {
                ScheduleStep.FORM -> ScheduleForm(editor, state, viewModel)
                ScheduleStep.DATE -> ScheduleDatePage(editor, viewModel)
                ScheduleStep.TIME -> ScheduleTimePage(editor, viewModel)
            }
        }
    }
}

@Composable
private fun ScheduleForm(editor: ScheduleEditor, state: ItemUiState, viewModel: ItemViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        OrbitFilterChip(
            label = stringResource(Res.string.schedule_due),
            selected = !editor.window,
            onClick = { viewModel.setScheduleWindow(false) },
        )
        OrbitFilterChip(
            label = stringResource(Res.string.schedule_window),
            selected = editor.window,
            onClick = { viewModel.setScheduleWindow(true) },
        )
    }
    val start = itemDateLabel(editor.date)
    val rows = listOfNotNull(
        ScheduleRow(
            title = stringResource(if (editor.window) Res.string.schedule_from else Res.string.schedule_on, start),
            icon = Res.drawable.ic_event,
            shape = OrbitShape.Cookie,
            set = true,
            onClick = { viewModel.pickScheduleDate(ScheduleDate.START) },
        ),
        if (editor.window) {
            ScheduleRow(
                title = stringResource(Res.string.schedule_to, itemDateLabel(editor.endDate)),
                icon = Res.drawable.ic_event,
                shape = OrbitShape.Clover,
                set = true,
                onClick = { viewModel.pickScheduleDate(ScheduleDate.END) },
            )
        } else {
            ScheduleRow(
                title = editor.time.ifBlank { stringResource(Res.string.schedule_add_time) },
                icon = Res.drawable.ic_clock,
                shape = OrbitShape.Sunny,
                set = editor.time.isNotBlank(),
                onClick = viewModel::pickScheduleTime,
            )
        },
    )
    OrbitGroup<Any>(rows + DURATION, key = { (it as? ScheduleRow)?.shape ?: DURATION }, modifier = Modifier.fillMaxWidth()) { row ->
        if (row is ScheduleRow) {
            OrbitActionRow(
                title = row.title,
                supporting = stringResource(if (row.set) Res.string.schedule_tap_to_change else Res.string.schedule_tap_to_set),
                icon = row.icon,
                shape = row.shape,
                onClick = row.onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DurationRow(editor, viewModel)
        }
    }
    if (ItemError.EndNotAfterStart in editor.errors) {
        OrbitErrorText(stringResource(Res.string.schedule_end_not_after_start))
    }
    if (state.failed) {
        OrbitErrorText(stringResource(Res.string.item_action_failed))
    }
    OrbitButton(
        text = stringResource(Res.string.schedule_save),
        onClick = viewModel::saveSchedule,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canSaveSchedule,
    )
    if (state.schedule != null) {
        OrbitTextButton(
            text = stringResource(Res.string.schedule_remove),
            onClick = viewModel::removeSchedule,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
        )
    }
}

/** How long it takes, as a row like the dates above it, with the wheels where those have their arrow. */
@Composable
private fun DurationRow(editor: ScheduleEditor, viewModel: ItemViewModel) {
    OrbitGroupRow(Modifier.fillMaxWidth()) {
        OrbitShapeBadge(OrbitTheme.accent.vivid, OrbitShape.Flower) {
            Icon(painterResource(Res.drawable.ic_clock), contentDescription = null, tint = Ink)
        }
        Text(
            text = stringResource(Res.string.schedule_how_long),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        OrbitTimeWheel(
            hours = editor.durationHours,
            minutes = editor.durationMinutes,
            onHoursChange = viewModel::onDurationHoursChange,
            onMinutesChange = viewModel::onDurationMinutesChange,
            hoursLabel = stringResource(Res.string.duration_unit_hours),
            minutesLabel = stringResource(Res.string.duration_unit_minutes),
        )
    }
}

/** The picker's own state holds the day being browsed; the view model gets the one confirmed. */
// DatePicker is experimental on iOS only (stable on Android): an exception AGENTS.md allows.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDatePage(editor: ScheduleEditor, viewModel: ItemViewModel) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = editor.pickerMillis,
        yearRange = editor.pickerYears,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = editor.canPick(utcTimeMillis)

            override fun isSelectableYear(year: Int) = editor.canPickYear(year)
        },
    )
    val scheme = MaterialTheme.colorScheme
    DatePicker(
        state = pickerState,
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
        text = stringResource(Res.string.schedule_pick),
        onClick = { pickerState.selectedDateMillis?.let(viewModel::onScheduleDatePicked) },
        modifier = Modifier.fillMaxWidth(),
        enabled = pickerState.selectedDateMillis != null,
    )
}

/** The picker's own state holds the time being set; the view model gets the one confirmed. */
// TimePicker is experimental in material3 1.9.0: an exception AGENTS.md allows, here and nowhere else.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePage(editor: ScheduleEditor, viewModel: ItemViewModel) {
    val pickerState = rememberTimePickerState(editor.pickerTime.hour, editor.pickerTime.minute)
    val scheme = MaterialTheme.colorScheme
    val vivid = OrbitTheme.accent.vivid
    TimePicker(
        state = pickerState,
        colors = TimePickerDefaults.colors(
            clockDialColor = scheme.surfaceContainer,
            selectorColor = vivid,
            clockDialSelectedContentColor = Ink,
            clockDialUnselectedContentColor = scheme.onSurface,
            containerColor = scheme.surfaceBright,
            periodSelectorSelectedContainerColor = vivid,
            periodSelectorSelectedContentColor = Ink,
            periodSelectorUnselectedContainerColor = scheme.surfaceContainer,
            periodSelectorUnselectedContentColor = scheme.onSurface,
            timeSelectorSelectedContainerColor = vivid,
            timeSelectorSelectedContentColor = Ink,
            timeSelectorUnselectedContainerColor = scheme.surfaceContainer,
            timeSelectorUnselectedContentColor = scheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    OrbitButton(
        text = stringResource(Res.string.schedule_pick),
        onClick = { viewModel.onScheduleTimePicked(pickerState.hour, pickerState.minute) },
        modifier = Modifier.fillMaxWidth(),
    )
    OrbitTextButton(
        text = stringResource(Res.string.schedule_no_time),
        onClick = viewModel::clearScheduleTime,
        modifier = Modifier.fillMaxWidth(),
    )
}
