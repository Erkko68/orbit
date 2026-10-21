package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.member_change_role
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_members_load_failed
import orbit.shared.generated.resources.space_members_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The members of a space with their roles, loaded by a view model of its own. An owner or admin gets a
 * "Change role" button on every member whose role they can change.
 */
@Composable
internal fun SpaceMembersTab(
    spaceId: String,
    modifier: Modifier = Modifier,
    viewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
        InviteCard(spaceId)
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.space_members_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                title = stringResource(Res.string.space_members_load_failed),
                body = stringResource(Res.string.load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.members.forEachIndexed { index, member ->
            OrbitListItem(
                title = member.name,
                supporting = stringResource(member.role),
                leading = {
                    OrbitAvatar(member.initials, OrbitTheme.colors.spaceAccent(index), photoUrl = member.photoUrl)
                },
                trailing = if (member.canChangeRole) {
                    {
                        TextButton(onClick = { viewModel.openRolePicker(member.id) }) {
                            Text(stringResource(Res.string.member_change_role))
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
    state.rolePicker?.let { member ->
        RolePickerDialog(
            member = member,
            busy = state.busy,
            failed = state.failed,
            onPick = viewModel::changeRole,
            onDismiss = viewModel::closeRolePicker,
        )
    }
}
