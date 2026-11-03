package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.spacehome.settings.SpaceSettings
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_members_load_failed
import orbit.shared.generated.resources.space_members_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The members of a space as a row of faces, each in their colour, with the settings of the space to their right.
 * Their roles and the invitation live in the settings. [onGone] runs once the user has left or deleted the space.
 */
@Composable
internal fun SpaceMembersRow(
    spaceId: String,
    onGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.loading -> OrbitLoadingState(stringResource(Res.string.space_members_loading), modifier.fillMaxWidth())
        state.loadFailed -> OrbitErrorState(
            title = stringResource(Res.string.space_members_load_failed),
            body = stringResource(Res.string.load_failed_body),
            retryText = stringResource(Res.string.retry),
            onRetry = viewModel::retry,
            modifier = modifier.fillMaxWidth(),
        )

        else -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                itemsIndexed(state.members, key = { _, member -> member.id }) { index, member ->
                    OrbitAvatar(member.initials, OrbitTheme.colors.spaceAccent(index), photoUrl = member.photoUrl)
                }
            }
            SpaceSettings(spaceId, onGone)
        }
    }
}
