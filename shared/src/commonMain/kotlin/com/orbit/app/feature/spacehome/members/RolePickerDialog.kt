package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.domain.model.Role
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.member_role_failed
import orbit.shared.generated.resources.member_role_make_admin
import orbit.shared.generated.resources.member_role_make_member
import orbit.shared.generated.resources.member_role_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The roles somebody can be given. Owner is not one of them: ownership moves only through a transfer (#27). */
private val AssignableRoles = listOf(Role.ADMIN, Role.MEMBER)

/**
 * Picks the role of [member]: one button per role they can be given, the one they have being disabled.
 * [onPick] runs with the choice. While it is [busy] the choices wait, and [failed] says the last one did
 * not go through.
 */
@Composable
internal fun RolePickerDialog(
    member: SpaceMember,
    busy: Boolean,
    failed: Boolean,
    onPick: (Role) -> Unit,
    onDismiss: () -> Unit,
) {
    OrbitDialog(
        title = stringResource(Res.string.member_role_title, member.name),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
    ) {
        for (role in AssignableRoles) {
            OrbitOutlinedButton(
                text = stringResource(role.makeLabel()),
                onClick = { onPick(role) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && role != member.currentRole,
            )
        }
        if (failed) {
            Text(
                text = stringResource(Res.string.member_role_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun Role.makeLabel(): StringResource = when (this) {
    Role.ADMIN -> Res.string.member_role_make_admin
    else -> Res.string.member_role_make_member
}
