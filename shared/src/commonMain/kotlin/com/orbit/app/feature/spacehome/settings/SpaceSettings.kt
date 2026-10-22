package com.orbit.app.feature.spacehome.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitColorPicker
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.space_colour_coral
import orbit.shared.generated.resources.space_colour_sun
import orbit.shared.generated.resources.space_colour_teal
import orbit.shared.generated.resources.space_colour_violet
import orbit.shared.generated.resources.space_settings
import orbit.shared.generated.resources.space_settings_colour
import orbit.shared.generated.resources.space_settings_failed
import orbit.shared.generated.resources.space_settings_name
import orbit.shared.generated.resources.space_settings_save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The button that edits a space, and the dialog it opens: the name, and the colour from the accent palette
 * of the theme. It is for the top bar of the space and shows nothing to anyone but an owner or admin.
 */
@Composable
internal fun SpaceSettings(
    spaceId: String,
    modifier: Modifier = Modifier,
    viewModel: SpaceSettingsViewModel = koinViewModel(key = "settings-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.canEdit) return
    OrbitIconButton(Res.drawable.ic_edit, stringResource(Res.string.space_settings), viewModel::open, modifier)
    if (state.open) {
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
        }
    }
}
