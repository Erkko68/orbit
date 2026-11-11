package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_hand_over
import orbit.shared.generated.resources.account_delete_password
import orbit.shared.generated.resources.account_delete_title
import orbit.shared.generated.resources.account_delete_warning
import orbit.shared.generated.resources.close
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before deleting the account, and for the password Firebase wants to see again first. A failure stays
 * in the sheet, as does the list of spaces to hand over before the account can go (#188).
 */
@Composable
internal fun DeleteAccountSheet(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.account_delete_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.account_delete),
        onConfirm = viewModel::deleteAccount,
        confirmEnabled = state.canDelete,
        danger = true,
    ) {
        Text(
            text = stringResource(Res.string.account_delete_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hasPassword) {
            OrbitPasswordField(
                value = state.deletePassword,
                onValueChange = viewModel::onDeletePasswordChange,
                placeholder = stringResource(Res.string.account_delete_password),
                visible = state.passwordsVisible,
                onToggleVisibility = viewModel::togglePasswordsVisibility,
                modifier = Modifier.fillMaxWidth(),
                isError = state.deletePasswordError != null,
                supportingText = state.deletePasswordError?.let { stringResource(it) },
            )
        }
        if (state.handOver.isNotEmpty()) {
            OrbitErrorText(stringResource(Res.string.account_delete_hand_over, state.handOver.joinToString(", ")))
        }
        state.dialogError?.let {
            OrbitErrorText(stringResource(it))
        }
    }
}
