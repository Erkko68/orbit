package com.orbit.app.feature.spacesettings.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCodeTiles
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.share.TextSharer
import com.orbit.app.domain.model.INVITATION_CODE_LENGTH
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.invite_code_label
import orbit.shared.generated.resources.invite_renew
import orbit.shared.generated.resources.invite_share
import orbit.shared.generated.resources.invite_share_message
import orbit.shared.generated.resources.invite_title
import orbit.shared.generated.resources.invite_valid
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * The sheet where an owner or admin invites people to the space, in one step: it opens on the code, made for it if there
 * is none, which shares through the system share sheet with its link. A new code replaces the old one.
 */
@Composable
internal fun InviteSheet(state: InviteUiState, viewModel: InviteViewModel, sharer: TextSharer) {
    OrbitSheet(
        title = stringResource(Res.string.invite_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismiss,
    ) {
        InviteContent(state, viewModel, sharer)
    }
}

@Composable
private fun InviteContent(state: InviteUiState, viewModel: InviteViewModel, sharer: TextSharer) {
    val invitation = state.invitation
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Text(
            text = stringResource(Res.string.invite_code_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrbitCodeTiles(invitation?.code, INVITATION_CODE_LENGTH, Modifier.fillMaxWidth())
        Text(
            text = stringResource(Res.string.invite_valid),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.failure?.let { OrbitErrorText(stringResource(it.message)) }
    if (invitation == null) {
        // Only a failed first code leaves nothing to share: the sheet makes one as it opens.
        if (state.failure != null) {
            OrbitButton(
                text = stringResource(Res.string.retry),
                onClick = viewModel::createInvitation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
        }
        return
    }
    val message = stringResource(Res.string.invite_share_message, state.spaceName, invitation.link, invitation.code)
    OrbitButton(
        text = stringResource(Res.string.invite_share),
        onClick = { sharer.share(message) },
        modifier = Modifier.fillMaxWidth(),
    )
    OrbitTextButton(
        text = stringResource(Res.string.invite_renew),
        onClick = viewModel::renewInvitation,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy,
    )
}
