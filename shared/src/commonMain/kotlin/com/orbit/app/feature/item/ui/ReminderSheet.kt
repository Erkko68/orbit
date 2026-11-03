package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_notifications_off
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_reminder
import orbit.shared.generated.resources.duration_custom
import orbit.shared.generated.resources.reminder_add
import orbit.shared.generated.resources.reminder_none
import org.jetbrains.compose.resources.stringResource

/**
 * When to be reminded before the item is due: no reminder, the usual moments or one typed by hand, which then shows with
 * the others. Each is saved as it is tapped.
 */
@Composable
internal fun ReminderSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.item_add_reminder),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissReminder,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            OrbitFilterChip(
                label = stringResource(Res.string.reminder_none),
                selected = state.reminderOffsets.isEmpty(),
                onClick = viewModel::clearReminder,
                icon = Res.drawable.ic_notifications_off,
            )
            for (offset in state.reminderChoices) {
                OrbitFilterChip(
                    label = itemReminderLabel(offset),
                    selected = offset in state.reminderOffsets,
                    onClick = { viewModel.toggleReminderOffset(offset) },
                )
            }
            OrbitFilterChip(
                label = stringResource(Res.string.duration_custom),
                selected = state.customReminder != null,
                onClick = viewModel::toggleCustomReminder,
            )
        }
        state.customReminder?.let { custom ->
            CustomDurationFields(
                custom = custom,
                onAmountChange = viewModel::onCustomReminderAmountChange,
                onUnitSelect = viewModel::selectCustomReminderUnit,
                addText = stringResource(Res.string.reminder_add),
                onAdd = viewModel::addCustomReminder,
            )
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}
