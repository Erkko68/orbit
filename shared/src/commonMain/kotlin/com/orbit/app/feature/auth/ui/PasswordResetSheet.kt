package com.orbit.app.feature.auth.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.feature.auth.SignInUiState
import com.orbit.app.feature.auth.SignInViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.reset_body
import orbit.shared.generated.resources.reset_email
import orbit.shared.generated.resources.reset_send
import orbit.shared.generated.resources.reset_sent
import orbit.shared.generated.resources.reset_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks where to send a link for a new password (#217). Once sent it says the same thing for every
 * address, so a stranger cannot use it to find out who has an account here.
 */
@Composable
internal fun PasswordResetSheet(state: SignInUiState, viewModel: SignInViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.reset_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissPasswordReset,
        confirmText = if (state.resetSent) null else stringResource(Res.string.reset_send),
        onConfirm = viewModel::sendPasswordReset,
        confirmEnabled = state.canSendReset,
    ) {
        if (state.resetSent) {
            Text(
                stringResource(Res.string.reset_sent, state.resetEmail.trim()),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(stringResource(Res.string.reset_body), style = MaterialTheme.typography.bodyLarge)
            AuthEmailField(
                state.resetEmail,
                viewModel::onResetEmailChange,
                state.resetEmailError,
                placeholder = Res.string.reset_email,
            )
            state.resetFailure?.let { OrbitErrorText(stringResource(it)) }
        }
    }
}
