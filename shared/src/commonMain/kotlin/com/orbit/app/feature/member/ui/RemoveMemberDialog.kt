package com.orbit.app.feature.member.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.feature.member.SpaceMember
import com.orbit.app.feature.member.SpaceMembersUiState
import com.orbit.app.feature.member.SpaceMembersViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.member_remove_body
import orbit.shared.generated.resources.member_remove_cancel
import orbit.shared.generated.resources.member_remove_confirm
import orbit.shared.generated.resources.member_remove_failed
import orbit.shared.generated.resources.member_remove_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before taking a member out of the space (#264). The name is in the question, so a tap on the wrong
 * row is caught before anyone loses their space.
 */
@Composable
internal fun RemoveMemberDialog(
    member: SpaceMember,
    spaceName: String,
    members: SpaceMembersUiState,
    viewModel: SpaceMembersViewModel,
) {
    OrbitDialog(
        title = stringResource(Res.string.member_remove_title, member.name),
        dismissText = stringResource(Res.string.member_remove_cancel),
        onDismiss = viewModel::cancelRemove,
        confirmText = stringResource(Res.string.member_remove_confirm),
        onConfirm = viewModel::removeMember,
        confirmEnabled = !members.busy,
    ) {
        Text(
            stringResource(Res.string.member_remove_body, member.name, spaceName),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (members.roleFailed(member.id)) {
            OrbitErrorText(stringResource(Res.string.member_remove_failed))
        }
    }
}
