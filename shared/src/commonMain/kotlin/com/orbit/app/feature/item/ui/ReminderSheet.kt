package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitNumberWheel
import com.orbit.app.core.designsystem.component.OrbitOptionWheel
import com.orbit.app.core.designsystem.component.OrbitShapeBadge
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitSheetPages
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.DurationUnit
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.duration_amount
import orbit.shared.generated.resources.duration_custom
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_close
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_reminder
import orbit.shared.generated.resources.reminder_add
import orbit.shared.generated.resources.reminder_remove
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

private const val ADD = "add"

/**
 * The item's reminders as a list, each one removable, with the row that adds one last. Adding slides to the usual
 * moments the item does not have yet, and a moment of the user's own on the wheels. Each change is saved at once.
 */
@Composable
internal fun ReminderSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(if (state.addingReminder) Res.string.reminder_add else Res.string.item_add_reminder),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissReminder,
        backDescription = stringResource(Res.string.back),
        onBack = if (state.addingReminder) viewModel::closeAddReminder else null,
    ) {
        OrbitSheetPages(state.addingReminder) { adding ->
            if (adding) AddReminderPage(state, viewModel) else ReminderList(state, viewModel)
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}

@Composable
private fun ReminderList(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitGroup<Any>(state.reminderOffsets + ADD, key = { it }, modifier = Modifier.fillMaxWidth()) { row ->
        if (row is Duration) {
            OrbitGroupRow(Modifier.fillMaxWidth()) {
                OrbitShapeBadge(OrbitTheme.accent.vivid, OrbitShape.Sunny) {
                    Icon(painterResource(Res.drawable.ic_notifications), contentDescription = null, tint = Ink)
                }
                val label = itemReminderLabel(row)
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitIconButton(
                    Res.drawable.ic_close,
                    stringResource(Res.string.reminder_remove, label),
                    { viewModel.removeReminderOffset(row) },
                )
            }
        } else {
            OrbitActionRow(
                title = stringResource(Res.string.reminder_add),
                icon = Res.drawable.ic_add,
                onClick = viewModel::openAddReminder,
                modifier = Modifier.fillMaxWidth(),
                shape = OrbitShape.Clover,
            )
        }
    }
}

@Composable
private fun AddReminderPage(state: ItemUiState, viewModel: ItemViewModel) {
    if (state.reminderChoices.isNotEmpty()) {
        OrbitGroup(state.reminderChoices, key = { it }, modifier = Modifier.fillMaxWidth()) { offset ->
            OrbitActionRow(
                title = itemReminderLabel(offset),
                icon = Res.drawable.ic_notifications,
                onClick = { viewModel.addReminderOffset(offset) },
                modifier = Modifier.fillMaxWidth(),
                shape = OrbitShape.Sunny,
            )
        }
    }
    val custom = state.customReminder
    OrbitGroup(listOf(ADD), key = { it }, modifier = Modifier.fillMaxWidth()) {
        OrbitGroupRow(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.duration_custom), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            OrbitNumberWheel(
                value = custom.amount,
                onValueChange = viewModel::onCustomReminderAmountChange,
                description = stringResource(Res.string.duration_amount),
            )
            OrbitOptionWheel(
                options = DurationUnit.entries.map { stringResource(it.label) },
                selected = custom.unit.ordinal,
                onSelect = { viewModel.selectCustomReminderUnit(DurationUnit.entries[it]) },
            )
        }
    }
    OrbitButton(
        text = stringResource(Res.string.reminder_add),
        onClick = viewModel::addCustomReminder,
        modifier = Modifier.fillMaxWidth(),
        enabled = custom.length != null && custom.length !in state.reminderOffsets,
    )
}
