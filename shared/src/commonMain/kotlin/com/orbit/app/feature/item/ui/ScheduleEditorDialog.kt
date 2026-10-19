package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.ItemError
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.ScheduleDate
import com.orbit.app.feature.item.ScheduleEditor
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.module_schedule_title
import orbit.shared.generated.resources.schedule_due
import orbit.shared.generated.resources.schedule_duration
import orbit.shared.generated.resources.schedule_duration_invalid
import orbit.shared.generated.resources.schedule_end_not_after_start
import orbit.shared.generated.resources.schedule_from
import orbit.shared.generated.resources.schedule_on
import orbit.shared.generated.resources.schedule_pick
import orbit.shared.generated.resources.schedule_save
import orbit.shared.generated.resources.schedule_time
import orbit.shared.generated.resources.schedule_time_invalid
import orbit.shared.generated.resources.schedule_to
import orbit.shared.generated.resources.schedule_window
import org.jetbrains.compose.resources.stringResource

/**
 * Edits an item's schedule: a due date with an optional time, or a window of dates, and an optional
 * duration. The dates open M3's date picker; the time is typed because M3's time picker is experimental.
 */
// DatePickerDialog is experimental on iOS only (stable on Android): an exception AGENTS.md allows.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleEditorDialog(editor: ScheduleEditor, state: ItemUiState, viewModel: ItemViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.module_schedule_title),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissScheduleEditor,
    ) {
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
        OrbitOutlinedButton(
            text = stringResource(if (editor.window) Res.string.schedule_from else Res.string.schedule_on, start),
            onClick = { viewModel.pickScheduleDate(ScheduleDate.START) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (editor.window) {
            OrbitOutlinedButton(
                text = stringResource(Res.string.schedule_to, itemDateLabel(editor.endDate)),
                onClick = { viewModel.pickScheduleDate(ScheduleDate.END) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (ItemError.EndNotAfterStart in editor.errors) {
                ErrorText(stringResource(Res.string.schedule_end_not_after_start))
            }
        } else {
            OrbitTextField(
                value = editor.time,
                onValueChange = viewModel::onScheduleTimeChange,
                placeholder = stringResource(Res.string.schedule_time),
                modifier = Modifier.fillMaxWidth(),
                isError = editor.timeInvalid,
                supportingText = if (editor.timeInvalid) stringResource(Res.string.schedule_time_invalid) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        OrbitTextField(
            value = editor.duration,
            onValueChange = viewModel::onScheduleDurationChange,
            placeholder = stringResource(Res.string.schedule_duration),
            modifier = Modifier.fillMaxWidth(),
            isError = editor.durationInvalid,
            supportingText = if (editor.durationInvalid) stringResource(Res.string.schedule_duration_invalid) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (state.failed) {
            ErrorText(stringResource(Res.string.item_action_failed))
        }
        OrbitButton(
            text = stringResource(Res.string.schedule_save),
            onClick = viewModel::saveSchedule,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSaveSchedule,
        )
    }
    if (editor.picking != null) {
        // The picker's own state holds the day being browsed; the view model gets the one confirmed.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = editor.pickerMillis)
        DatePickerDialog(
            onDismissRequest = viewModel::dismissScheduleDatePicker,
            confirmButton = {
                OrbitTextButton(
                    text = stringResource(Res.string.schedule_pick),
                    onClick = { pickerState.selectedDateMillis?.let(viewModel::onScheduleDatePicked) },
                    enabled = pickerState.selectedDateMillis != null,
                )
            },
            dismissButton = {
                OrbitTextButton(stringResource(Res.string.cancel), viewModel::dismissScheduleDatePicker)
            },
        ) {
            DatePicker(pickerState)
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
