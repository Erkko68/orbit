package com.orbit.app.feature.spacesettings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import com.orbit.app.feature.member.SpaceMember
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.feature.spacesettings.SpaceSettingsUiState
import com.orbit.app.feature.spacesettings.SpaceSettingsViewModel
import com.orbit.app.feature.spacesettings.invite.InviteSheet
import com.orbit.app.feature.spacesettings.invite.InviteViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.delete_space_body
import orbit.shared.generated.resources.delete_space_confirm
import orbit.shared.generated.resources.delete_space_failed
import orbit.shared.generated.resources.delete_space_title
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_logout
import orbit.shared.generated.resources.ic_person_add
import orbit.shared.generated.resources.invite_title
import orbit.shared.generated.resources.leave_body
import orbit.shared.generated.resources.leave_confirm
import orbit.shared.generated.resources.leave_failed
import orbit.shared.generated.resources.leave_title
import orbit.shared.generated.resources.member_you
import orbit.shared.generated.resources.members_members
import orbit.shared.generated.resources.space_settings
import orbit.shared.generated.resources.space_settings_delete
import orbit.shared.generated.resources.space_settings_delete_body
import orbit.shared.generated.resources.space_settings_failed
import orbit.shared.generated.resources.space_settings_invite_body
import orbit.shared.generated.resources.space_settings_leave
import orbit.shared.generated.resources.space_settings_leave_body
import orbit.shared.generated.resources.space_settings_name
import orbit.shared.generated.resources.space_settings_save
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The settings of a space, a screen of its own: an owner or admin edits the name and colour, which Save writes, then
 * everyone sees the members, a tap on one opening their screen through [onOpenMember]. An owner or admin invites
 * people in a sheet. At the bottom the owner deletes the space and anyone else leaves it, after a dialog, and then
 * [onGone] runs.
 */
@Composable
fun SpaceSettingsScreen(
    spaceId: String,
    onBack: () -> Unit,
    onOpenMember: (String) -> Unit,
    onGone: () -> Unit,
    viewModel: SpaceSettingsViewModel = koinViewModel { parametersOf(spaceId) },
    membersViewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
    inviteViewModel: InviteViewModel = koinViewModel(key = "invite-$spaceId") { parametersOf(spaceId) },
    sharer: TextSharer = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val members by membersViewModel.uiState.collectAsStateWithLifecycle()
    val invite by inviteViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.gone) {
        if (state.gone) onGone()
    }
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.space_settings),
                navigationIcon = { OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack) },
            )
        },
        scrollable = true,
    ) {
        if (state.canEdit) Identity(state, viewModel)
        if (members.members.isNotEmpty()) {
            OrbitSectionHeader(
                title = stringResource(Res.string.members_members),
                modifier = Modifier.fillMaxWidth(),
                count = members.members.size.toString(),
            )
            OrbitGroup(members.byRole.flatMap { it.members }, key = { it.id }, modifier = Modifier.fillMaxWidth()) {
                MemberRow(it, onOpenMember)
            }
        }
        // What the user can do with the space, as rows of one group like the account's (#293).
        val actions = listOfNotNull(
            SpaceAction(
                title = stringResource(Res.string.invite_title),
                supporting = stringResource(Res.string.space_settings_invite_body),
                icon = Res.drawable.ic_person_add,
                shape = OrbitShape.Cookie,
                onClick = inviteViewModel::open,
            ).takeIf { invite.canInvite },
            SpaceAction(
                title = stringResource(if (state.deletes) Res.string.space_settings_delete else Res.string.space_settings_leave),
                supporting = stringResource(
                    if (state.deletes) Res.string.space_settings_delete_body else Res.string.space_settings_leave_body,
                ),
                icon = if (state.deletes) Res.drawable.ic_delete else Res.drawable.ic_logout,
                shape = OrbitShape.Burst,
                onClick = viewModel::confirmExit,
                danger = true,
            ).takeIf { state.role != null },
        )
        if (actions.isNotEmpty()) {
            OrbitGroup(actions, key = { it.title }, modifier = Modifier.fillMaxWidth()) { action ->
                OrbitActionRow(
                    title = action.title,
                    supporting = action.supporting,
                    icon = action.icon,
                    shape = action.shape,
                    danger = action.danger,
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (invite.open) InviteSheet(invite, inviteViewModel, sharer)
    if (state.confirmingExit) ExitDialog(state, viewModel)
}

/** One thing the user can do with the space. */
private class SpaceAction(
    val title: String,
    val supporting: String,
    val icon: DrawableResource,
    val shape: OrbitShape,
    val onClick: () -> Unit,
    val danger: Boolean = false,
)

/** The name, which an owner or admin changes. Save shows once something differs from what is saved. */
@Composable
private fun Identity(state: SpaceSettingsUiState, viewModel: SpaceSettingsViewModel) {
    OrbitTextField(
        value = state.name,
        onValueChange = viewModel::onNameChange,
        placeholder = stringResource(Res.string.space_settings_name),
        modifier = Modifier.fillMaxWidth(),
        isError = state.failed,
        supportingText = if (state.failed) stringResource(Res.string.space_settings_failed) else null,
        leadingIcon = Res.drawable.ic_home,
    )
    if (state.changed) {
        OrbitButton(
            text = stringResource(Res.string.space_settings_save),
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSave,
        )
    }
}

@Composable
private fun MemberRow(member: SpaceMember, onOpen: (String) -> Unit) {
    OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { onOpen(member.id) }) {
        OrbitAvatar(
            initials = member.initials,
            accent = OrbitTheme.colors.spaceAccent(member.index),
            photoUrl = member.photoUrl,
            shape = memberShape(member.index),
            size = OrbitAvatarSize.Large,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = if (member.isYou) stringResource(Res.string.member_you) else member.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(member.role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.exitFailed) {
            OrbitErrorText(stringResource(if (state.deletes) Res.string.delete_space_failed else Res.string.leave_failed))
        }
    }
}
