package com.orbit.app.feature.member.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitCheck
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.item.label
import com.orbit.app.feature.member.SpaceMember
import com.orbit.app.feature.member.SpaceMembersUiState
import com.orbit.app.feature.member.SpaceMembersViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.member_role
import orbit.shared.generated.resources.member_role_failed
import orbit.shared.generated.resources.member_role_make_owner
import orbit.shared.generated.resources.or
import org.jetbrains.compose.resources.stringResource

/**
 * Picks [member]'s role, for the owner: member or admin, a tap saving it. Under a divider an admin can be made the
 * owner, which the handover dialog asks about first.
 */
@Composable
internal fun RoleSheet(member: SpaceMember, state: SpaceMembersUiState, viewModel: SpaceMembersViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.member_role),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissRoles,
    ) {
        OrbitGroup(listOf(Role.MEMBER, Role.ADMIN), key = { it }, modifier = Modifier.fillMaxWidth()) { role ->
            val name = stringResource(role.label)
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.changeRole(member.id, role) }) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitCheck(
                    checked = member.currentRole == role,
                    onCheckedChange = { viewModel.changeRole(member.id, role) },
                    contentDescription = name,
                    enabled = !state.busy,
                    compact = true,
                )
            }
        }
        if (member.canTakeOwnership) {
            OrbitLabeledDivider(stringResource(Res.string.or))
            OrbitTonalButton(
                text = stringResource(Res.string.member_role_make_owner),
                onClick = { viewModel.askTransfer(member.id) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
        }
        if (state.roleFailed(member.id)) {
            OrbitErrorText(stringResource(Res.string.member_role_failed))
        }
    }
}
