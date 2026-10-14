package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitHeading
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSelectionDialog
import com.orbit.app.core.designsystem.component.OrbitSelectionField
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_back
import orbit.shared.generated.resources.account_display_name
import orbit.shared.generated.resources.account_language
import orbit.shared.generated.resources.account_language_ca
import orbit.shared.generated.resources.account_language_en
import orbit.shared.generated.resources.account_language_es
import orbit.shared.generated.resources.account_save
import orbit.shared.generated.resources.account_save_failed
import orbit.shared.generated.resources.account_saved
import orbit.shared.generated.resources.account_saving
import orbit.shared.generated.resources.account_sign_out
import orbit.shared.generated.resources.account_sign_out_failed
import orbit.shared.generated.resources.account_signing_out
import orbit.shared.generated.resources.account_subtitle
import orbit.shared.generated.resources.account_time_zone
import orbit.shared.generated.resources.account_time_zone_hint
import orbit.shared.generated.resources.account_time_zone_invalid
import orbit.shared.generated.resources.account_time_zone_select
import orbit.shared.generated.resources.account_time_zone_search
import orbit.shared.generated.resources.account_time_zone_empty
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Edits the current user's profile on confirmation. Session changes handle navigation after logout. */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nameLabel = stringResource(Res.string.account_display_name)
    val zoneLabel = stringResource(Res.string.account_time_zone)
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.account_title),
                navigationIcon = {
                    OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.account_back), onBack)
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        scrollable = true,
    ) {
        OrbitCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            OrbitAvatar(
                state.userInitials,
                accent = MaterialTheme.colorScheme.primary,
                photoUrl = state.user?.photoUrl,
            )
            OrbitHeading(
                title = state.user?.displayName.orEmpty(),
                subtitle = stringResource(Res.string.account_subtitle),
                titleStyle = MaterialTheme.typography.titleLarge,
            )
            OrbitTextField(
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                placeholder = nameLabel,
                label = nameLabel,
                enabled = state.canEdit,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameLabel },
            )
            Text(stringResource(Res.string.account_language), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                for (language in listOf("es", "ca", "en")) {
                    val label = when (language) {
                        "es" -> Res.string.account_language_es
                        "ca" -> Res.string.account_language_ca
                        else -> Res.string.account_language_en
                    }
                    OrbitFilterChip(
                        label = stringResource(label),
                        selected = state.language == language,
                        onClick = { viewModel.selectLanguage(language) },
                        enabled = state.canEdit,
                    )
                }
            }
            OrbitSelectionField(
                value = state.timeZone,
                onClick = viewModel::openTimeZonePicker,
                label = zoneLabel,
                enabled = state.canEdit,
                isError = state.timeZoneInvalid,
                supportingText = stringResource(
                    if (state.timeZoneInvalid) Res.string.account_time_zone_invalid else Res.string.account_time_zone_hint,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.saveFailed) {
                Text(stringResource(Res.string.account_save_failed), color = MaterialTheme.colorScheme.error)
            }
            if (state.saved) {
                Text(stringResource(Res.string.account_saved), color = MaterialTheme.colorScheme.onSurface)
            }
            OrbitButton(
                text = stringResource(if (state.saving) Res.string.account_saving else Res.string.account_save),
                onClick = viewModel::saveProfile,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OrbitTextButton(
            text = stringResource(if (state.signingOut) Res.string.account_signing_out else Res.string.account_sign_out),
            onClick = viewModel::signOut,
            enabled = state.canEdit,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.signOutFailed) {
            Text(stringResource(Res.string.account_sign_out_failed), color = MaterialTheme.colorScheme.error)
        }
    }
    if (state.timeZonePickerOpen) {
        OrbitSelectionDialog(
            title = stringResource(Res.string.account_time_zone_select),
            dismissText = stringResource(Res.string.cancel),
            searchLabel = stringResource(Res.string.account_time_zone_search),
            emptyText = stringResource(Res.string.account_time_zone_empty),
            query = state.timeZoneQuery,
            options = state.timeZoneOptions,
            onQueryChange = viewModel::onTimeZoneQueryChange,
            onSelect = viewModel::onTimeZoneChange,
            onDismiss = viewModel::closeTimeZonePicker,
        )
    }
}
