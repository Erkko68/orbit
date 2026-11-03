package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitCheck
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.member_role_failed
import orbit.shared.generated.resources.transfer_acknowledge
import orbit.shared.generated.resources.transfer_body
import orbit.shared.generated.resources.transfer_cancel
import orbit.shared.generated.resources.transfer_confirm
import orbit.shared.generated.resources.transfer_title
import org.jetbrains.compose.resources.stringResource

/** Asks before handing the space over, which the user cannot take back: they tick that only [member] can give it back. */
@Composable
internal fun TransferDialog(
    member: SpaceMember,
    spaceName: String,
    members: SpaceMembersUiState,
    viewModel: SpaceMembersViewModel,
) {
    val acknowledge = stringResource(Res.string.transfer_acknowledge, member.name)
    OrbitDialog(
        title = stringResource(Res.string.transfer_title, spaceName, member.name),
        dismissText = stringResource(Res.string.transfer_cancel),
        onDismiss = viewModel::cancelTransfer,
        confirmText = stringResource(Res.string.transfer_confirm),
        onConfirm = viewModel::transferOwnership,
        confirmEnabled = members.canTransfer,
    ) {
        Text(stringResource(Res.string.transfer_body, member.name), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            OrbitCheck(
                checked = members.transferAcknowledged,
                onCheckedChange = viewModel::acknowledgeTransfer,
                contentDescription = acknowledge,
            )
            Text(acknowledge, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        }
        if (members.roleFailed(member.id)) {
            OrbitErrorText(stringResource(Res.string.member_role_failed))
        }
    }
}
