package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_change_password
import orbit.shared.generated.resources.account_change_password_body
import orbit.shared.generated.resources.account_current_password
import orbit.shared.generated.resources.account_new_password
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.create_account_confirm
import org.jetbrains.compose.resources.stringResource

/** Changes the password: the current one, then the new one twice. Each error shows under its field. */
@Composable
internal fun ChangePasswordDialog(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.account_change_password),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.account_change_password),
        onConfirm = viewModel::changePassword,
        confirmEnabled = !state.submitting,
    ) {
        OrbitPasswordField(
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
