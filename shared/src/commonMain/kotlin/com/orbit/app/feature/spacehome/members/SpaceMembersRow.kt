package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitIconBadge
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitMemberChip
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_person_add
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.member_invite
import orbit.shared.generated.resources.member_you
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_members_load_failed
import orbit.shared.generated.resources.space_members_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The members of a space as a row of chips, with their roles, and an "Invite" chip at the end for an owner or
 * admin. Tapping another member opens their role picker when the user may change it or hand them the space.
 */
@Composable
internal fun SpaceMembersRow(
    spaceId: String,
    modifier: Modifier = Modifier,
    viewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
    inviteViewModel: InviteViewModel = koinViewModel(key = "invite-$spaceId") { parametersOf(spaceId) },
    sharer: TextSharer = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val invite by inviteViewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.loading -> OrbitLoadingState(stringResource(Res.string.space_members_loading), modifier.fillMaxWidth())
        state.loadFailed -> OrbitErrorState(
            title = stringResource(Res.string.space_members_load_failed),
            body = stringResource(Res.string.load_failed_body),
            retryText = stringResource(Res.string.retry),
            onRetry = viewModel::retry,
            modifier = modifier.fillMaxWidth(),
        )

        else -> LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            itemsIndexed(state.members, key = { _, member -> member.id }) { index, member ->
                val tappable = member.canChangeRole || member.canTakeOwnership
                OrbitMemberChip(
                    name = if (member.isYou) stringResource(Res.string.member_you) else member.name,
                    detail = stringResource(member.role),
                    leading = {
                        OrbitAvatar(member.initials, OrbitTheme.colors.spaceAccent(index), photoUrl = member.photoUrl)
                    },
                    highlighted = member.isYou,
                    onClick = if (tappable) ({ viewModel.openRolePicker(member.id) }) else null,
                )
            }
            if (invite.canInvite) {
                item(key = "invite") {
                    OrbitMemberChip(
                        name = stringResource(Res.string.member_invite),
                        leading = { OrbitIconBadge(Res.drawable.ic_person_add) },
                        highlighted = true,
                        onClick = inviteViewModel::open,
                    )
                }
            }
        }
    }
    state.rolePicker?.let { member ->
        RolePickerDialog(
            member = member,
            busy = state.busy,
            failed = state.failed,
            onPick = viewModel::changeRole,
            onTransfer = viewModel::transferOwnership,
            onDismiss = viewModel::closeRolePicker,
        )
    }
    if (invite.open && invite.canInvite) {
        InviteDialog(invite, inviteViewModel, sharer)
    }
}
