package com.orbit.app.feature.spacehome.members

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitHeaderAction
import com.orbit.app.core.designsystem.component.OrbitSheetColorHeader
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Role
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_undo
import orbit.shared.generated.resources.member_role_make_admin
import orbit.shared.generated.resources.member_role_make_member
import orbit.shared.generated.resources.member_role_make_owner
import org.jetbrains.compose.resources.stringResource

/**
 * The top of the settings sheet on one [member]'s page, in their colour: their face, name and role, in [spaceName].
 * For the owner it also changes the role in place: a member is made admin, and an admin member again or the owner,
 * through [onChangeRole] and [onMakeOwner]. While a change is [busy] the buttons wait.
 */
@Composable
internal fun MemberHeader(
    member: SpaceMember,
    spaceName: String,
    busy: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onChangeRole: (Role) -> Unit,
    onMakeOwner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitSpaceColor(member.index) {
        OrbitSheetColorHeader(
            title = member.name,
            label = spaceName,
            closeDescription = stringResource(Res.string.close),
            onClose = onClose,
            modifier = modifier,
            backDescription = stringResource(Res.string.back),
            onBack = onBack,
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
            detail = stringResource(member.role).uppercase(),
            actions = if (member.canChangeRole) {
                { RoleActions(member, busy, onChangeRole, onMakeOwner) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun RoleActions(member: SpaceMember, busy: Boolean, onChangeRole: (Role) -> Unit, onMakeOwner: () -> Unit) {
    if (member.currentRole == Role.MEMBER) {
        OrbitHeaderAction(
            text = stringResource(Res.string.member_role_make_admin),
            icon = Res.drawable.ic_lock,
            onClick = { onChangeRole(Role.ADMIN) },
            enabled = !busy,
        )
    }
    if (member.canTakeOwnership) {
        OrbitHeaderAction(
            text = stringResource(Res.string.member_role_make_owner),
            icon = Res.drawable.ic_home,
            onClick = onMakeOwner,
            enabled = !busy,
        )
    }
    if (member.currentRole == Role.ADMIN) {
        OrbitHeaderAction(
            text = stringResource(Res.string.member_role_make_member),
            icon = Res.drawable.ic_undo,
            onClick = { onChangeRole(Role.MEMBER) },
            enabled = !busy,
            quiet = true,
        )
    }
}
