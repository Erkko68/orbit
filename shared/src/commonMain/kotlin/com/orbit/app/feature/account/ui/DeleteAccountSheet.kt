package com.orbit.app.feature.account.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_title
import orbit.shared.generated.resources.account_delete_warning
import orbit.shared.generated.resources.close
import org.jetbrains.compose.resources.stringResource

/** Asks before deleting the account. A failure stays in the sheet. */
@Composable
internal fun DeleteAccountSheet(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.account_delete_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.account_delete),
        onConfirm = viewModel::deleteAccount,
        confirmEnabled = !state.submitting,
        danger = true,
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
