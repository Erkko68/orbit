package com.orbit.app.feature.account.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_connect_google
import orbit.shared.generated.resources.account_connect_google_warning
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.sign_in_google
import org.jetbrains.compose.resources.stringResource

/** Connects a Google account as another way in, through Google's own picker. A failure stays in the sheet. */
@Composable
internal fun ConnectGoogleSheet(state: AccountUiState, viewModel: AccountViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.account_connect_google),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissDialog,
        confirmText = stringResource(Res.string.sign_in_google),
        onConfirm = viewModel::connectGoogle,
        confirmEnabled = !state.submitting,
    ) {
        Text(
            text = stringResource(Res.string.account_connect_google_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.dialogError?.let {
            OrbitErrorText(stringResource(it))
        }
    }
}
