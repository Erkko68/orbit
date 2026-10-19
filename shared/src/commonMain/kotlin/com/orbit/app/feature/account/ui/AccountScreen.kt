package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitListCard
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.account.AccountDialog
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_back
import orbit.shared.generated.resources.account_change_password
import orbit.shared.generated.resources.account_change_password_body
import orbit.shared.generated.resources.account_current_password
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_body
import orbit.shared.generated.resources.account_delete_title
import orbit.shared.generated.resources.account_delete_warning
import orbit.shared.generated.resources.account_edit_name
import orbit.shared.generated.resources.account_name
import orbit.shared.generated.resources.account_new_password
import orbit.shared.generated.resources.account_save
import orbit.shared.generated.resources.account_sign_out
import orbit.shared.generated.resources.account_sign_out_body
import orbit.shared.generated.resources.account_sign_out_failed
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.create_account_confirm
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_logout
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's account: who they are on an elevated card, then the account's actions, each a
 * card that opens its dialog. Signing out or deleting the account moves the auth gate to sign-in.
 */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.account_title),
                navigationIcon = {
                    OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.account_back), onBack)
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        scrollable = true,
    ) {
        OrbitCard(Modifier.fillMaxWidth(), elevated = true) {
            OrbitListItem(
                title = state.name,
                supporting = state.email,
                leading = { OrbitAvatar(state.initials, MaterialTheme.colorScheme.primary, photoUrl = state.photoUrl) },
                trailing = {
                    OrbitIconButton(
                        icon = Res.drawable.ic_edit,
                        contentDescription = stringResource(Res.string.account_edit_name),
                        onClick = viewModel::openEditName,
                    )
                },
            )
        }
        OrbitListCard(
            title = stringResource(Res.string.account_change_password),
            supporting = stringResource(Res.string.account_change_password_body),
            icon = Res.drawable.ic_lock,
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::openChangePassword,
        )
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitListCard(
                title = stringResource(Res.string.account_sign_out),
                supporting = stringResource(Res.string.account_sign_out_body),
                icon = Res.drawable.ic_logout,
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::signOut,
            )
            if (state.signOutFailed) {
                OrbitErrorText(stringResource(Res.string.account_sign_out_failed))
            }
            OrbitListCard(
                title = stringResource(Res.string.account_delete),
                supporting = stringResource(Res.string.account_delete_body),
                icon = Res.drawable.ic_delete,
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::openDeleteAccount,
                iconTint = MaterialTheme.colorScheme.error,
            )
        }
    }
    when (state.dialog) {
        AccountDialog.EDIT_NAME -> EditNameDialog(state, viewModel)
        AccountDialog.CHANGE_PASSWORD -> ChangePasswordDialog(state, viewModel)
        AccountDialog.DELETE_ACCOUNT -> DeleteAccountDialog(state, viewModel)
        null -> Unit
    }
}

@Composable
private fun EditNameDialog(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.account_edit_name),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.account_save),
        onConfirm = viewModel::saveName,
        confirmEnabled = state.canSaveName,
    ) {
        OrbitTextField(
            value = state.nameDraft,
            onValueChange = viewModel::onNameChange,
            placeholder = stringResource(Res.string.account_name),
            modifier = Modifier.fillMaxWidth(),
            isError = state.dialogError != null,
            supportingText = state.dialogError?.let { stringResource(it) },
        )
    }
}

@Composable
private fun ChangePasswordDialog(state: AccountUiState, viewModel: AccountViewModel) {
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

@Composable
private fun DeleteAccountDialog(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.account_delete_title),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.account_delete),
        onConfirm = viewModel::deleteAccount,
        confirmEnabled = !state.submitting,
    ) {
        Text(
            text = stringResource(Res.string.account_delete_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.dialogError?.let {
            OrbitErrorText(stringResource(it))
        }
    }
}
