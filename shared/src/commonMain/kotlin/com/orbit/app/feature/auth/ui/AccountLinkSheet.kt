package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.feature.auth.AccountLinkUiState
import com.orbit.app.feature.auth.AuthLinkForm
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.sign_in_google
import org.jetbrains.compose.resources.stringResource

/**
 * The short task of making one account out of two ways in (#173): the address already has an account,
 * and the user proves it is theirs. With a password account they type the password and Google is linked
 * to it; with a Google account they continue with Google, or set a password for that same account, the
 * two alternatives of one goal in one sheet.
 */
@Composable
internal fun AccountLinkSheet(
    state: AccountLinkUiState,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OrbitSheet(
        title = stringResource(state.title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = onDismiss,
        confirmText = stringResource(state.confirm),
        onConfirm = onConfirm,
        confirmEnabled = state.canLink,
    ) {
        Text(
            text = stringResource(state.body, state.email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.googleOffered) {
            OrbitTonalButton(
                text = stringResource(Res.string.sign_in_google),
                onClick = onContinueWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.linking,
            )
            OrbitLabeledDivider(stringResource(Res.string.or))
        }
        OrbitPasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(state.passwordPlaceholder),
            visible = state.passwordVisible,
            onToggleVisibility = onToggleVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { stringResource(it) },
        )
    }
}

/** Shows [AccountLinkSheet] while [state] says the address already has an account, for any auth form. */
@Composable
internal fun AccountLinkSheetFor(state: AccountLinkUiState?, form: AuthLinkForm) {
    state?.let { link ->
        AccountLinkSheet(
            state = link,
            onPasswordChange = form::onLinkPasswordChange,
            onToggleVisibility = form::toggleLinkPasswordVisibility,
            onContinueWithGoogle = form::signInWithGoogle,
            onConfirm = form::confirmLink,
            onDismiss = form::dismissLink,
        )
    }
}
