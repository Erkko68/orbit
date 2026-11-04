package com.orbit.app.feature.space.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAddButton
import com.orbit.app.core.designsystem.component.OrbitAvatarButton
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitHero
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.feature.space.card.SpaceCard
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.spaces_empty
import orbit.shared.generated.resources.spaces_empty_body
import orbit.shared.generated.resources.spaces_load_failed
import orbit.shared.generated.resources.spaces_loading
import orbit.shared.generated.resources.spaces_new
import orbit.shared.generated.resources.spaces_section
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's spaces, one [SpaceCard] each, under a big title and the Orbit bar with the user's account.
 * Tapping a space calls [onOpenSpace] with its id. The add button opens the screen that creates or joins one, as does an invitation link that was opened.
 */
@Composable
fun SpaceScreen(
    onOpenSpace: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onNewSpace: () -> Unit,
    viewModel: SpaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.app_name),
                showMark = true,
                actions = {
                    OrbitAvatarButton(
                        initials = state.userInitials,
                        photoUrl = state.userPhotoUrl,
                        accent = OrbitTheme.accent.fill,
                        contentDescription = stringResource(Res.string.account_title),
                        onClick = onOpenAccount,
                    )
                },
            )
        },
        floatingActionButton = {
            OrbitAddButton(stringResource(Res.string.spaces_new), onNewSpace)
        },
        scrollable = true,
    ) {
        OrbitHero(stringResource(Res.string.spaces_section), Modifier.fillMaxWidth())
        if (state.loading) {
            OrbitLoadingState(stringResource(Res.string.spaces_loading), Modifier.fillMaxWidth())
        } else if (state.loadFailed) {
            OrbitErrorState(
                title = stringResource(Res.string.spaces_load_failed),
                body = stringResource(Res.string.load_failed_body),
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
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            for (spaceId in state.spaceIds) {
                key(spaceId) { SpaceCard(spaceId, onClick = { onOpenSpace(spaceId) }, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
    LaunchedEffect(state.invitationWaiting) {
        if (state.invitationWaiting) onNewSpace()
    }
}
