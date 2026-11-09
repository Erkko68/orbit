package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitDatePicker
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitNumberWheel
import com.orbit.app.core.designsystem.component.OrbitOptionWheel
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
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.RepeatUnit
import com.orbit.app.feature.item.ScheduleDate
import com.orbit.app.feature.item.ScheduleEditor
import kotlinx.datetime.DayOfWeek
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.duration_amount
import orbit.shared.generated.resources.duration_custom
import orbit.shared.generated.resources.duration_unit_hours
import orbit.shared.generated.resources.duration_unit_minutes
import orbit.shared.generated.resources.ic_clock
import orbit.shared.generated.resources.ic_check
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_repeat
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.module_schedule_title
import orbit.shared.generated.resources.repeat_done
import orbit.shared.generated.resources.repeat_ends
import orbit.shared.generated.resources.repeat_every
import orbit.shared.generated.resources.repeat_month_day
import orbit.shared.generated.resources.repeat_never
import orbit.shared.generated.resources.repeat_none
import orbit.shared.generated.resources.repeat_unit_days
import orbit.shared.generated.resources.repeat_unit_months
import orbit.shared.generated.resources.repeat_unit_years
import orbit.shared.generated.resources.repeat_unit_weeks
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
import orbit.shared.generated.resources.schedule_repeat
import orbit.shared.generated.resources.schedule_save
import orbit.shared.generated.resources.schedule_tap_to_change
import orbit.shared.generated.resources.schedule_tap_to_set
import orbit.shared.generated.resources.schedule_to
import orbit.shared.generated.resources.schedule_window
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val DURATION = "duration"

private enum class ScheduleStep { FORM, DATE, TIME, REPEAT, CUSTOM_REPEAT }

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
        editor.customRepeat -> ScheduleStep.CUSTOM_REPEAT
        editor.editingRepeat -> ScheduleStep.REPEAT
        else -> ScheduleStep.FORM
    }
    OrbitSheet(
        title = stringResource(
            when (step) {
                ScheduleStep.FORM -> Res.string.module_schedule_title
                ScheduleStep.DATE -> Res.string.schedule_pick_date
                ScheduleStep.TIME -> Res.string.schedule_pick_time
                ScheduleStep.REPEAT -> Res.string.schedule_repeat
                ScheduleStep.CUSTOM_REPEAT -> Res.string.duration_custom
            },
        ),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissScheduleEditor,
        backDescription = stringResource(Res.string.back),
        onBack = when (step) {
            ScheduleStep.FORM -> null
            ScheduleStep.DATE -> viewModel::dismissScheduleDatePicker
            ScheduleStep.TIME -> viewModel::dismissScheduleTimePicker
            ScheduleStep.REPEAT -> viewModel::closeRepeat
            ScheduleStep.CUSTOM_REPEAT -> viewModel::closeCustomRepeat
        },
    ) {
        OrbitSheetPages(step) { shown ->
            when (shown) {
                ScheduleStep.FORM -> ScheduleForm(editor, state, viewModel)
                ScheduleStep.DATE -> ScheduleDatePage(editor, viewModel)
                ScheduleStep.TIME -> ScheduleTimePage(editor, viewModel)
                ScheduleStep.REPEAT -> RepeatPresetsPage(editor, viewModel)
                ScheduleStep.CUSTOM_REPEAT -> CustomRepeatPage(editor, viewModel)
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
        ScheduleRow(
            title = editor.recurrence?.let { itemRecurrenceLabel(it) } ?: stringResource(Res.string.schedule_repeat),
            icon = Res.drawable.ic_repeat,
            shape = OrbitShape.Burst,
            set = editor.repeat != null,
            onClick = viewModel::openRepeat,
        ),
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

/** The usual repeats as rows, the one picked ticked, and a repeat of the user's own last. Picking one goes back. */
@Composable
private fun RepeatPresetsPage(editor: ScheduleEditor, viewModel: ItemViewModel) {
    val custom = editor.isCustomRepeat
    OrbitGroup<Any>(editor.repeatPresets.map { Preset(it) } + CUSTOM, key = { (it as? Preset)?.recurrence ?: it }, modifier = Modifier.fillMaxWidth()) { row ->
        if (row is Preset) {
            val picked = !custom && editor.recurrence == row.recurrence
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.pickRepeat(row.recurrence) }) {
                Text(
                    text = row.recurrence?.let { itemRecurrenceLabel(it) } ?: stringResource(Res.string.repeat_none),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (picked) Icon(painterResource(Res.drawable.ic_check), contentDescription = null)
            }
        } else {
            OrbitActionRow(
                title = stringResource(Res.string.duration_custom),
                supporting = editor.recurrence?.takeIf { custom }?.let { itemRecurrenceLabel(it) },
                icon = Res.drawable.ic_repeat,
                shape = OrbitShape.Burst,
                onClick = viewModel::openCustomRepeat,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A usual repeat, or not repeating: a class so that null has a key in the group. */
private data class Preset(val recurrence: Recurrence?)

private const val CUSTOM = "custom"

private enum class RepeatRow { EVERY, WEEKDAYS, ENDS }

/** Every how many of what on two wheels, the days of a weekly repeat, and when it ends. */
@Composable
private fun CustomRepeatPage(editor: ScheduleEditor, viewModel: ItemViewModel) {
    val repeat = editor.repeat ?: return
    val rows = listOfNotNull(RepeatRow.EVERY, RepeatRow.WEEKDAYS.takeIf { repeat == RepeatUnit.WEEKS }, RepeatRow.ENDS)
    OrbitGroup(rows, key = { it }, modifier = Modifier.fillMaxWidth()) { row ->
        when (row) {
            RepeatRow.EVERY -> EveryRow(editor, repeat, viewModel)
            RepeatRow.WEEKDAYS -> WeekdaysRow(editor, viewModel)
            RepeatRow.ENDS -> OrbitActionRow(
                title = stringResource(Res.string.repeat_ends),
                supporting = editor.repeatUntil?.let { itemDateLabel(it) } ?: stringResource(Res.string.repeat_never),
                icon = Res.drawable.ic_event,
                shape = OrbitShape.Clover,
                onClick = { viewModel.pickScheduleDate(ScheduleDate.UNTIL) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    OrbitButton(
        text = stringResource(Res.string.repeat_done),
        onClick = viewModel::finishCustomRepeat,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** "Every" on a number wheel beside a wheel of days, weeks, months and years. A monthly or yearly repeat says its day. */
@Composable
private fun EveryRow(editor: ScheduleEditor, repeat: RepeatUnit, viewModel: ItemViewModel) {
    val every = editor.repeatEvery
    OrbitGroupRow(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.repeat_every), style = MaterialTheme.typography.titleMedium)
            if (repeat == RepeatUnit.MONTHS || repeat == RepeatUnit.YEARS) {
                Text(
                    text = stringResource(Res.string.repeat_month_day, editor.date.day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OrbitNumberWheel(
            value = every,
            onValueChange = viewModel::onRepeatEveryChange,
            description = stringResource(Res.string.duration_amount),
        )
        OrbitOptionWheel(
            options = RepeatUnit.entries.map { repeatUnitLabel(it, every) },
            selected = repeat.ordinal,
            onSelect = { viewModel.selectRepeatUnit(RepeatUnit.entries[it]) },
        )
    }
}

@Composable
private fun repeatUnitLabel(unit: RepeatUnit, every: Int): String = pluralStringResource(
    when (unit) {
        RepeatUnit.DAYS -> Res.plurals.repeat_unit_days
        RepeatUnit.WEEKS -> Res.plurals.repeat_unit_weeks
        RepeatUnit.MONTHS -> Res.plurals.repeat_unit_months
        RepeatUnit.YEARS -> Res.plurals.repeat_unit_years
    },
    every,
)

/** The days of a weekly repeat, Monday first. The last one picked stays. */
@Composable
private fun WeekdaysRow(editor: ScheduleEditor, viewModel: ItemViewModel) {
    OrbitGroupRow(Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
            DayOfWeek.entries.forEach { day ->
                OrbitFilterChip(
                    label = itemWeekdayLabel(day),
                    selected = day in editor.repeatWeekdays,
                    onClick = { viewModel.toggleRepeatWeekday(day) },
                )
            }
        }
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

/** The calendar of the date being picked; Never under it clears the end of a repeat. */
@Composable
private fun ScheduleDatePage(editor: ScheduleEditor, viewModel: ItemViewModel) {
    OrbitDatePicker(
        selectedMillis = editor.pickerMillis,
        pickText = stringResource(Res.string.schedule_pick),
        onPick = viewModel::onScheduleDatePicked,
        years = editor.pickerYears,
        canPick = editor::canPick,
        canPickYear = editor::canPickYear,
    )
    if (editor.picking == ScheduleDate.UNTIL) {
        OrbitTextButton(
            text = stringResource(Res.string.repeat_never),
            onClick = viewModel::clearRepeatUntil,
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
