package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_change_password
import orbit.shared.generated.resources.account_change_password_body
import orbit.shared.generated.resources.account_current_password
import orbit.shared.generated.resources.account_new_password
import orbit.shared.generated.resources.account_set_password
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.create_account_confirm
import org.jetbrains.compose.resources.stringResource

/**
 * Changes the password: the current one, then the new one twice. Each error shows under its field. An account that
 * only signs in with Google has no current one, so the same sheet sets its first password (#293).
 */
@Composable
internal fun ChangePasswordSheet(state: AccountUiState, viewModel: AccountViewModel) {
    val title = stringResource(if (state.hasPassword) Res.string.account_change_password else Res.string.account_set_password)
    OrbitSheet(
        title = title,
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissDialog,
        confirmText = title,
        onConfirm = viewModel::changePassword,
        confirmEnabled = !state.submitting,
    ) {
        if (state.hasPassword) OrbitPasswordField(
            value = state.currentPassword,
            onValueChange = viewModel::onCurrentPasswordChange,
            placeholder = stringResource(Res.string.account_current_password),
            visible = state.passwordsVisible,
            onToggleVisibility = viewModel::togglePasswordsVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = state.currentPasswordError != null,
            supportingText = state.currentPasswordError?.let { stringResource(it) },
            imeAction = ImeAction.Next,
        )
        OrbitPasswordField(
            value = state.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            placeholder = stringResource(Res.string.account_new_password),
            visible = state.passwordsVisible,
            onToggleVisibility = viewModel::togglePasswordsVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = state.newPasswordError != null,
            // The rule is shown from the start, and turns into the error when it is broken.
            supportingText = stringResource(state.newPasswordError ?: Res.string.account_change_password_body),
            imeAction = ImeAction.Next,
        )
        OrbitPasswordField(
            value = state.confirmation,
            onValueChange = viewModel::onConfirmationChange,
            placeholder = stringResource(Res.string.create_account_confirm),
            visible = state.passwordsVisible,
            onToggleVisibility = viewModel::togglePasswordsVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = state.confirmationError != null,
            supportingText = state.confirmationError?.let { stringResource(it) },
        )
    }
}
