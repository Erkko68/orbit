package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitPasswordField
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitScreenHeader
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.auth.SignInViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_mail
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.sign_in
import orbit.shared.generated.resources.sign_in_apple
import orbit.shared.generated.resources.sign_in_create_account
import orbit.shared.generated.resources.sign_in_email
import orbit.shared.generated.resources.sign_in_failed
import orbit.shared.generated.resources.sign_in_forgot_password
import orbit.shared.generated.resources.sign_in_google
import orbit.shared.generated.resources.sign_in_new
import orbit.shared.generated.resources.sign_in_password
import orbit.shared.generated.resources.sign_in_subtitle
import orbit.shared.generated.resources.sign_in_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Sign-in as one centered column: the Orbit header, the form, then the other ways in.
 * Placeholders until their issues land: forgot password, Google, Apple and create account are
 * disabled, and there is no validation yet.
 */
@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        scrollable = true,
    ) {
        OrbitScreenHeader(stringResource(Res.string.sign_in_title), stringResource(Res.string.sign_in_subtitle))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(Res.string.sign_in_email),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(painterResource(Res.drawable.ic_mail), contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            OrbitPasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = stringResource(Res.string.sign_in_password),
                visible = state.passwordVisible,
                onToggleVisibility = viewModel::togglePasswordVisibility,
                modifier = Modifier.fillMaxWidth(),
                isError = state.failed,
                supportingText = if (state.failed) stringResource(Res.string.sign_in_failed) else null,
            )
            OrbitButton(
                text = stringResource(Res.string.sign_in),
                onClick = viewModel::signIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.submitting,
            )
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally), enabled = false) {
                Text(stringResource(Res.string.sign_in_forgot_password))
            }
        }
        OrbitLabeledDivider(stringResource(Res.string.or))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitOutlinedButton(
                text = stringResource(Res.string.sign_in_google),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            OrbitOutlinedButton(
                text = stringResource(Res.string.sign_in_apple),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sign_in_new),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {}, enabled = false) {
                Text(stringResource(Res.string.sign_in_create_account))
            }
        }
    }
}
