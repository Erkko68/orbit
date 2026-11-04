package com.orbit.app.feature.member.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitColorHeader
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.member.SpaceMember
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.feature.member.activity.MemberActivity
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.member_load_failed
import orbit.shared.generated.resources.member_loading
import orbit.shared.generated.resources.member_not_found
import orbit.shared.generated.resources.member_not_found_body
import orbit.shared.generated.resources.member_role
import orbit.shared.generated.resources.member_role_failed
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One member of a space, a screen of its own: a header in their colour with their face, name and role, then, for the
 * owner, a Role row that opens the role sheet, and what they have and did lately, an item of which opens through
 * [onOpenItem].
 */
@Composable
fun MemberScreen(
    spaceId: String,
    userId: String,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val member = state.members.firstOrNull { it.id == userId }
    OrbitSpaceColor(member?.index) {
        OrbitScreen(
            topBar = {
                if (member != null) {
                    MemberHeader(member, state.spaceName, onBack)
                } else {
                    OrbitTopBar(
                        title = state.spaceName,
                        navigationIcon = { OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack) },
                    )
                }
            },
            scrollable = true,
        ) {
            when {
                member != null -> {
                    if (member.canChangeRole) {
                        OrbitActionRow(
                            title = stringResource(Res.string.member_role),
                            supporting = stringResource(member.role),
                            icon = Res.drawable.ic_lock,
                            onClick = { viewModel.openRoles(member.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.roleFailed(member.id) && state.pickingRole == null && state.transferTo == null) {
                        OrbitErrorText(stringResource(Res.string.member_role_failed))
                    }
                    MemberActivity(spaceId, userId, onOpenItem)
                }

                state.loading -> OrbitLoadingState(stringResource(Res.string.member_loading), Modifier.fillMaxWidth())
                state.loadFailed -> OrbitErrorState(
                    title = stringResource(Res.string.member_load_failed),
                    body = stringResource(Res.string.load_failed_body),
                    retryText = stringResource(Res.string.retry),
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxWidth(),
                )

                else -> OrbitEmptyState(
                    title = stringResource(Res.string.member_not_found),
                    body = stringResource(Res.string.member_not_found_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    state.pickingRole?.let { RoleSheet(it, state, viewModel) }
    state.transferTo?.let { TransferDialog(it, state.spaceName, state, viewModel) }
}

/** Shows who it is, in their colour: nothing in it is edited. */
@Composable
private fun MemberHeader(member: SpaceMember, spaceName: String, onBack: () -> Unit) {
    OrbitColorHeader(
        title = member.name,
        label = spaceName,
        backDescription = stringResource(Res.string.back),
        onBack = onBack,
        detail = stringResource(member.role).uppercase(),
        leading = {
            OrbitAvatar(
                initials = member.initials,
                accent = Ink,
                contentColor = OrbitTheme.accent.vivid,
                photoUrl = member.photoUrl,
                shape = memberShape(member.index),
                size = OrbitAvatarSize.Hero,
            )
        },
    )
}
