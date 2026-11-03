package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.share.TextSharer
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.invite_code_label
import orbit.shared.generated.resources.invite_create
import orbit.shared.generated.resources.invite_none
import orbit.shared.generated.resources.invite_revoke
import orbit.shared.generated.resources.invite_share
import orbit.shared.generated.resources.invite_share_message
import orbit.shared.generated.resources.invite_valid
import org.jetbrains.compose.resources.stringResource

/**
 * A page of the settings where an owner or admin invites people to the space: creates a link and a code, shares them
 * through the system share sheet and revokes them.
 */
@Composable
internal fun InvitePage(state: InviteUiState, viewModel: InviteViewModel, sharer: TextSharer) {
    val invitation = state.invitation
    if (invitation == null) {
        Text(
            text = stringResource(Res.string.invite_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrbitButton(
            text = stringResource(Res.string.invite_create),
            onClick = viewModel::createInvitation,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy && !state.loading,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            Text(
                text = stringResource(Res.string.invite_code_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(invitation.code, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = invitation.link,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.invite_valid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val message = stringResource(
            Res.string.invite_share_message,
            state.spaceName,
            invitation.link,
            invitation.code,
        )
        OrbitButton(
            text = stringResource(Res.string.invite_share),
            onClick = { sharer.share(message) },
            modifier = Modifier.fillMaxWidth(),
        )
        OrbitTonalButton(
            text = stringResource(Res.string.invite_revoke),
            onClick = viewModel::revokeInvitation,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
    }
    state.failure?.let {
        OrbitErrorText(stringResource(it.message))
    }
}
