package com.orbit.app.feature.space.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatarButton
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitExtendedFab
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.space.SpaceUiState
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.feature.space.card.SpaceCard
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_link
import orbit.shared.generated.resources.ic_orbit
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.spaces_create
import orbit.shared.generated.resources.spaces_create_failed
import orbit.shared.generated.resources.spaces_create_name
import orbit.shared.generated.resources.spaces_empty
import orbit.shared.generated.resources.spaces_empty_body
import orbit.shared.generated.resources.spaces_join
import orbit.shared.generated.resources.spaces_join_code
import orbit.shared.generated.resources.spaces_load_failed
import orbit.shared.generated.resources.spaces_load_failed_body
import orbit.shared.generated.resources.spaces_loading
import orbit.shared.generated.resources.spaces_count
import orbit.shared.generated.resources.spaces_new
import orbit.shared.generated.resources.spaces_section
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's spaces, one [SpaceCard] each, under the Orbit bar with the user's account.
 * Tapping a space calls [onOpenSpace] with its id. "New space" opens the dialog to create or join one.
 */
@Composable
fun SpaceScreen(
    onOpenSpace: (String) -> Unit,
    onOpenAccount: () -> Unit,
    viewModel: SpaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.app_name),
                navigationIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_orbit),
                        contentDescription = null,
                        modifier = Modifier.padding(start = OrbitSpacing.sm),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    OrbitAvatarButton(
                        initials = state.userInitials,
                        photoUrl = state.userPhotoUrl,
                        accent = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(Res.string.account_title),
                        onClick = onOpenAccount,
                        modifier = Modifier.padding(end = OrbitSpacing.sm),
                    )
                },
            )
        },
        floatingActionButton = {
            OrbitExtendedFab(
                text = stringResource(Res.string.spaces_new),
                icon = Res.drawable.ic_add,
                onClick = viewModel::openCreate,
            )
        },
        scrollable = true,
    ) {
        if (state.loading) {
            OrbitLoadingState(stringResource(Res.string.spaces_loading), Modifier.fillMaxWidth())
        } else if (state.loadFailed) {
            OrbitErrorState(
                title = stringResource(Res.string.spaces_load_failed),
                body = stringResource(Res.string.spaces_load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (state.spaceIds.isEmpty()) {
            OrbitEmptyState(
                title = stringResource(Res.string.spaces_empty),
                body = stringResource(Res.string.spaces_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.spaceCount > 0) {
            OrbitSectionHeader(
                title = stringResource(Res.string.spaces_section),
                icon = Res.drawable.ic_home,
                modifier = Modifier.fillMaxWidth(),
                count = pluralStringResource(Res.plurals.spaces_count, state.spaceCount, state.spaceCount),
            )
        }
        for (spaceId in state.spaceIds) {
            key(spaceId) {
                SpaceCard(spaceId, onClick = { onOpenSpace(spaceId) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (state.createOpen) {
        NewSpaceDialog(state, viewModel)
    }
}

/** One way in for a new space: name one to create it, or join one with an invite (a placeholder until #25). */
@Composable
private fun NewSpaceDialog(state: SpaceUiState, viewModel: SpaceViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.spaces_new),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissCreate,
    ) {
        OrbitTextField(
            value = state.newSpaceName,
            onValueChange = viewModel::onNewSpaceNameChange,
            placeholder = stringResource(Res.string.spaces_create_name),
            modifier = Modifier.fillMaxWidth(),
            isError = state.createFailed,
            supportingText = if (state.createFailed) stringResource(Res.string.spaces_create_failed) else null,
            leadingIcon = Res.drawable.ic_home,
        )
        OrbitButton(
            text = stringResource(Res.string.spaces_create),
            onClick = viewModel::createSpace,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canCreate,
        )
        OrbitLabeledDivider(stringResource(Res.string.or))
        OrbitTextField(
            value = "",
            onValueChange = {},
            placeholder = stringResource(Res.string.spaces_join_code),
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            leadingIcon = Res.drawable.ic_link,
        )
        OrbitOutlinedButton(
            text = stringResource(Res.string.spaces_join),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        )
    }
}
