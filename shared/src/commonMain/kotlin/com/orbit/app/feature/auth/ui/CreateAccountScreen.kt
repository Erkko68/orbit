package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.feature.auth.CreateAccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.create_account
import orbit.shared.generated.resources.create_account_confirm
import orbit.shared.generated.resources.create_account_have_account
import orbit.shared.generated.resources.create_account_hint
import orbit.shared.generated.resources.create_account_subtitle
import orbit.shared.generated.resources.create_account_title
import orbit.shared.generated.resources.sign_in
import orbit.shared.generated.resources.sign_in_password
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Create account on the [AuthLayout], laid out like sign-in. "Sign in" calls [onSignIn]. Google and
 * Apple are disabled placeholders until their issues land.
 */
@Composable
fun CreateAccountScreen(onSignIn: () -> Unit, viewModel: CreateAccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val emailError = emailMessage(state.errors, state.authError)
    val passwordError = state.errors.firstOrNull { it == CredentialsError.ShortPassword }?.message()
        ?: otherAuthMessage(state.authError)
    val confirmationError = state.errors.firstOrNull { it == CredentialsError.PasswordsDiffer }?.message()
    AuthLayout(
        title = Res.string.create_account_title,
        subtitle = Res.string.create_account_subtitle,
        switchPrompt = Res.string.create_account_have_account,
        switchAction = Res.string.sign_in,
        onSwitch = onSignIn,
    ) {
        AuthEmailField(state.email, viewModel::onEmailChange, emailError)
        OrbitPasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = stringResource(Res.string.sign_in_password),
            visible = state.passwordVisible,
            onToggleVisibility = viewModel::togglePasswordVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError != null,
            // The rule is shown from the start, and turns into the error when it is broken.
            supportingText = stringResource(passwordError ?: Res.string.create_account_hint),
            imeAction = ImeAction.Next,
        )
        OrbitPasswordField(
            value = state.confirmation,
            onValueChange = viewModel::onConfirmationChange,
            placeholder = stringResource(Res.string.create_account_confirm),
            visible = state.confirmationVisible,
            onToggleVisibility = viewModel::toggleConfirmationVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = confirmationError != null,
            supportingText = confirmationError?.let { stringResource(it) },
        )
        OrbitButton(
            text = stringResource(Res.string.create_account),
            onClick = viewModel::createAccount,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
    }
}
