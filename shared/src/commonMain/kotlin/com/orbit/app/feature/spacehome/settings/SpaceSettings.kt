package com.orbit.app.feature.spacehome.settings

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
import com.orbit.app.core.designsystem.component.OrbitCrew
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFloatingFace
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitSheetPages
import com.orbit.app.core.designsystem.component.OrbitSpaceColorPicker
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import com.orbit.app.feature.spacehome.members.InvitePage
import com.orbit.app.feature.spacehome.members.InviteViewModel
import com.orbit.app.feature.spacehome.members.SpaceMember
import com.orbit.app.feature.spacehome.members.SpaceMembersUiState
import com.orbit.app.feature.spacehome.members.SpaceMembersViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.delete_space_body
import orbit.shared.generated.resources.delete_space_confirm
import orbit.shared.generated.resources.delete_space_failed
import orbit.shared.generated.resources.delete_space_title
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_group
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_person_add
import orbit.shared.generated.resources.invite_title
import orbit.shared.generated.resources.leave_body
import orbit.shared.generated.resources.leave_confirm
import orbit.shared.generated.resources.leave_failed
import orbit.shared.generated.resources.leave_title
import orbit.shared.generated.resources.member_you
import orbit.shared.generated.resources.space_settings
import orbit.shared.generated.resources.space_settings_delete
import orbit.shared.generated.resources.space_settings_failed
import orbit.shared.generated.resources.space_settings_identity
import orbit.shared.generated.resources.space_settings_identity_body
import orbit.shared.generated.resources.space_settings_invite
import orbit.shared.generated.resources.space_settings_invite_body
import orbit.shared.generated.resources.space_settings_leave
import orbit.shared.generated.resources.space_settings_members
import orbit.shared.generated.resources.space_settings_name
import orbit.shared.generated.resources.space_settings_save
import orbit.shared.generated.resources.spaces_members
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The members of a space floating around the settings in the top bar, any of them opening everything behind it: one sheet that changes in place, with what
 * the user can do with the space. An owner or admin invites people and edits the name and colour, anyone sees the
 * members and their roles, and at the bottom the owner deletes the space and anyone else leaves it, after confirming.
 * [onGone] runs once the user has left or deleted the space.
 */
@Composable
internal fun SpaceSettings(
    spaceId: String,
    onGone: () -> Unit,
    onOpenMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceSettingsViewModel = koinViewModel(key = "settings-$spaceId") { parametersOf(spaceId) },
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
    if (!state.visible) return
    OrbitCrew(
        faces = members.members.map { member ->
            OrbitFloatingFace(
                id = member.id,
                initials = member.initials,
                color = OrbitTheme.colors.spaceAccent(member.index),
                shape = memberShape(member.index),
                description = member.name,
                photoUrl = member.photoUrl,
            )
        },
        settingsDescription = stringResource(Res.string.space_settings),
        onClick = viewModel::open,
        modifier = modifier,
    )
    val page = state.page ?: return
    val step = when {
        state.confirmingExit -> SpaceSettingsStep.EXIT
        invite.open && invite.canInvite -> SpaceSettingsStep.INVITE
        page == SpaceSettingsPage.IDENTITY -> SpaceSettingsStep.IDENTITY
        page == SpaceSettingsPage.MEMBERS -> SpaceSettingsStep.MEMBERS
        else -> SpaceSettingsStep.MENU
    }
    OrbitSheet(
        title = stepTitle(step, state),
        closeDescription = stringResource(Res.string.close),
        onDismiss = {
            inviteViewModel.dismiss()
            viewModel.dismiss()
        },
        backDescription = stringResource(Res.string.back),
        onBack = when (step) {
            SpaceSettingsStep.MENU -> null
            SpaceSettingsStep.IDENTITY, SpaceSettingsStep.MEMBERS -> viewModel::backToMenu
            SpaceSettingsStep.INVITE -> inviteViewModel::dismiss
            SpaceSettingsStep.EXIT -> viewModel::cancelExit
        },
    ) {
        OrbitSheetPages(step) { shown ->
            when (shown) {
                SpaceSettingsStep.MENU ->
                    SettingsMenu(state, viewModel, members.members.size, invite.canInvite, inviteViewModel::open)
                SpaceSettingsStep.IDENTITY -> IdentityPage(state, viewModel)
                SpaceSettingsStep.MEMBERS -> MembersPage(members) { memberId ->
                    viewModel.dismiss()
                    onOpenMember(memberId)
                }
                SpaceSettingsStep.INVITE -> InvitePage(invite, inviteViewModel, sharer)
                SpaceSettingsStep.EXIT -> ExitPage(state, viewModel)
            }
        }
    }
}

@Composable
private fun stepTitle(step: SpaceSettingsStep, state: SpaceSettingsUiState): String = when (step) {
    SpaceSettingsStep.MENU -> state.spaceName
    SpaceSettingsStep.IDENTITY -> stringResource(Res.string.space_settings_identity)
    SpaceSettingsStep.MEMBERS -> stringResource(Res.string.space_settings_members)
    SpaceSettingsStep.INVITE -> stringResource(Res.string.invite_title)
    SpaceSettingsStep.EXIT ->
        stringResource(if (state.deletes) Res.string.delete_space_title else Res.string.leave_title)
}

/** One thing the menu of the settings offers. */
private class MenuAction(
    val title: String,
    val supporting: String,
    val icon: DrawableResource,
    val shape: OrbitShape,
    val onClick: () -> Unit,
)

@Composable
private fun SettingsMenu(
    state: SpaceSettingsUiState,
    viewModel: SpaceSettingsViewModel,
    memberCount: Int,
    canInvite: Boolean,
    onInvite: () -> Unit,
) {
    val invite = MenuAction(
        title = stringResource(Res.string.space_settings_invite),
        supporting = stringResource(Res.string.space_settings_invite_body),
        icon = Res.drawable.ic_person_add,
        shape = OrbitShape.Cookie,
        onClick = onInvite,
    )
    val members = MenuAction(
        title = stringResource(Res.string.space_settings_members),
        supporting = pluralStringResource(Res.plurals.spaces_members, memberCount, memberCount),
        icon = Res.drawable.ic_group,
        shape = OrbitShape.Clover,
        onClick = viewModel::openMembers,
    )
    val identity = MenuAction(
        title = stringResource(Res.string.space_settings_identity),
        supporting = stringResource(Res.string.space_settings_identity_body),
        icon = Res.drawable.ic_edit,
        shape = OrbitShape.Sunny,
        onClick = viewModel::openIdentity,
    )
    OrbitGroup(
        items = listOfNotNull(invite.takeIf { canInvite }, members, identity.takeIf { state.canEdit }),
        key = { it.title },
        modifier = Modifier.fillMaxWidth(),
    ) { action ->
        OrbitActionRow(
            title = action.title,
            supporting = action.supporting,
            icon = action.icon,
            shape = action.shape,
            onClick = action.onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    OrbitTextButton(
        text = stringResource(if (state.deletes) Res.string.space_settings_delete else Res.string.space_settings_leave),
        onClick = viewModel::confirmExit,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
    )
}

/** Who is in the space, a section per role. Tapping a member opens their page through [onOpen]. */
@Composable
private fun MembersPage(members: SpaceMembersUiState, onOpen: (String) -> Unit) {
    for (section in members.byRole) {
        OrbitSectionHeader(
            title = stringResource(section.title),
            modifier = Modifier.fillMaxWidth(),
            count = section.members.size.toString(),
        )
        OrbitGroup(items = section.members, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
            MemberRow(member, onOpen)
        }
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
        Text(
            text = if (member.isYou) stringResource(Res.string.member_you) else member.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun IdentityPage(state: SpaceSettingsUiState, viewModel: SpaceSettingsViewModel) {
    OrbitTextField(
        value = state.name,
        onValueChange = viewModel::onNameChange,
        placeholder = stringResource(Res.string.space_settings_name),
        modifier = Modifier.fillMaxWidth(),
        isError = state.failed,
        supportingText = if (state.failed) stringResource(Res.string.space_settings_failed) else null,
        leadingIcon = Res.drawable.ic_home,
    )
    OrbitSpaceColorPicker(state.accent, viewModel::onAccentChange)
    OrbitButton(
        text = stringResource(Res.string.space_settings_save),
        onClick = viewModel::save,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canSave,
    )
}

/** Asks before the owner deletes the space, or before anyone else leaves it. */
@Composable
private fun ExitPage(state: SpaceSettingsUiState, viewModel: SpaceSettingsViewModel) {
    Text(
        text = stringResource(if (state.deletes) Res.string.delete_space_body else Res.string.leave_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state.exitFailed) {
        OrbitErrorText(stringResource(if (state.deletes) Res.string.delete_space_failed else Res.string.leave_failed))
    }
    OrbitButton(
        text = stringResource(if (state.deletes) Res.string.delete_space_confirm else Res.string.leave_confirm),
        onClick = viewModel::exit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.exiting,
        danger = true,
    )
}
