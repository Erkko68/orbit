package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.feature.account.AccountUiState
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_edit_name
import orbit.shared.generated.resources.account_name
import orbit.shared.generated.resources.account_save
import orbit.shared.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

/** Renames the user. A failure shows under the field. */
@Composable
internal fun EditNameDialog(state: AccountUiState, viewModel: AccountViewModel) {
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
