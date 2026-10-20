package com.orbit.app.feature.account.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_title
import orbit.shared.generated.resources.account_delete_warning
import orbit.shared.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

/** Asks before deleting the account. A failure stays in the dialog. */
@Composable
internal fun DeleteAccountDialog(state: AccountUiState, viewModel: AccountViewModel) {
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
