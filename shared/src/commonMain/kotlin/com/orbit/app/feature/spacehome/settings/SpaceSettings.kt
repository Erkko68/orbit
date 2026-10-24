package com.orbit.app.feature.spacehome.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitColorPicker
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.delete_space_body
import orbit.shared.generated.resources.delete_space_confirm
import orbit.shared.generated.resources.delete_space_failed
import orbit.shared.generated.resources.delete_space_title
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_logout
import orbit.shared.generated.resources.leave_body
import orbit.shared.generated.resources.leave_confirm
import orbit.shared.generated.resources.leave_failed
import orbit.shared.generated.resources.leave_title
import orbit.shared.generated.resources.space_colour_coral
import orbit.shared.generated.resources.space_colour_sun
import orbit.shared.generated.resources.space_colour_teal
import orbit.shared.generated.resources.space_colour_violet
import orbit.shared.generated.resources.space_settings
import orbit.shared.generated.resources.space_settings_colour
import orbit.shared.generated.resources.space_settings_delete
import orbit.shared.generated.resources.space_settings_failed
import orbit.shared.generated.resources.space_settings_leave
import orbit.shared.generated.resources.space_settings_name
import orbit.shared.generated.resources.space_settings_save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The button that opens the settings of a space, for the top bar of the space, and its dialog. An owner or admin
 * edits the name and the colour from the accent palette of the theme; at the bottom the owner deletes the space
 * and an admin leaves it. A plain member has nothing to edit, so their button is "Leave space". Both ask to
 * confirm first, and [onGone] runs once the user has left or deleted the space.
 */
@Composable
internal fun SpaceSettings(
    spaceId: String,
    onGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceSettingsViewModel = koinViewModel(key = "settings-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.gone) {
        if (state.gone) onGone()
    }
    if (!state.visible) return
    if (state.canEdit) {
        OrbitIconButton(Res.drawable.ic_edit, stringResource(Res.string.space_settings), viewModel::open, modifier)
    } else {
        val leave = stringResource(Res.string.space_settings_leave)
        OrbitIconButton(Res.drawable.ic_logout, leave, viewModel::open, modifier)
    }
    when {
        !state.open -> Unit
        state.confirmingExit -> ExitDialog(state, viewModel)
        else -> EditDialog(state, viewModel)
    }
}

@Composable
private fun EditDialog(state: SpaceSettingsUiState, viewModel: SpaceSettingsViewModel) {
    val palette = OrbitTheme.colors.spaceAccents
    // One name per colour of the palette, in its order.
    val names = listOf(
        stringResource(Res.string.space_colour_violet),
        stringResource(Res.string.space_colour_coral),
        stringResource(Res.string.space_colour_teal),
        stringResource(Res.string.space_colour_sun),
    )
    OrbitDialog(
        title = stringResource(Res.string.space_settings),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismiss,
        confirmText = stringResource(Res.string.space_settings_save),
        onConfirm = viewModel::save,
        confirmEnabled = state.canSave,
    ) {
        OrbitTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = stringResource(Res.string.space_settings_name),
            modifier = Modifier.fillMaxWidth(),
            isError = state.failed,
            supportingText = if (state.failed) stringResource(Res.string.space_settings_failed) else null,
            leadingIcon = Res.drawable.ic_home,
        )
        Text(
            text = stringResource(Res.string.space_settings_colour),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrbitColorPicker(
            colors = palette,
            labels = names,
            selectedIndex = state.accent.mod(palette.size),
            onSelect = viewModel::onAccentChange,
        )
        OrbitOutlinedButton(
            text = stringResource(
                if (state.deletes) Res.string.space_settings_delete else Res.string.space_settings_leave,
            ),
            onClick = viewModel::confirmExit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.saving,
        )
    }
}

/** Asks before the owner deletes the space, or before anyone else leaves it. */
@Composable
private fun ExitDialog(state: SpaceSettingsUiState, viewModel: SpaceSettingsViewModel) {
    OrbitDialog(
        title = stringResource(if (state.deletes) Res.string.delete_space_title else Res.string.leave_title),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::cancelExit,
        confirmText = stringResource(if (state.deletes) Res.string.delete_space_confirm else Res.string.leave_confirm),
        onConfirm = viewModel::exit,
        confirmEnabled = !state.exiting,
    ) {
        Text(
            text = stringResource(if (state.deletes) Res.string.delete_space_body else Res.string.leave_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.exitFailed) {
            val failure = if (state.deletes) Res.string.delete_space_failed else Res.string.leave_failed
            OrbitErrorText(stringResource(failure))
        }
    }
}
