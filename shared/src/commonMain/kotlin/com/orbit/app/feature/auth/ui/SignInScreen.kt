package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.feature.auth.SignInViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.sign_in
import orbit.shared.generated.resources.sign_in_create_account
import orbit.shared.generated.resources.sign_in_new
import orbit.shared.generated.resources.sign_in_password
import orbit.shared.generated.resources.sign_in_subtitle
import orbit.shared.generated.resources.sign_in_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Sign-in on the [AuthLayout]. "Create account" calls [onCreateAccount]. */
@Composable
fun SignInScreen(onCreateAccount: () -> Unit, viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuthLayout(
        title = Res.string.sign_in_title,
        subtitle = Res.string.sign_in_subtitle,
        switchPrompt = Res.string.sign_in_new,
        switchAction = Res.string.sign_in_create_account,
        onSwitch = onCreateAccount,
        onGoogleSignIn = viewModel::signInWithGoogle,
        googleSignInEnabled = !state.submitting,
    ) {
        AuthEmailField(state.email, viewModel::onEmailChange, state.emailError)
        OrbitPasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = stringResource(Res.string.sign_in_password),
            visible = state.passwordVisible,
            onToggleVisibility = viewModel::togglePasswordVisibility,
            modifier = Modifier.fillMaxWidth(),
            isError = state.passwordError != null,
            supportingText = state.passwordError?.let { stringResource(it) },
        )
        OrbitButton(
            text = stringResource(Res.string.sign_in),
            onClick = viewModel::signIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
    }
}
