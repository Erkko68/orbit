package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.auth.SignInViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_mail
import orbit.shared.generated.resources.ic_visibility
import orbit.shared.generated.resources.ic_visibility_off
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.sign_in
import orbit.shared.generated.resources.sign_in_apple
import orbit.shared.generated.resources.sign_in_create_account
import orbit.shared.generated.resources.sign_in_email
import orbit.shared.generated.resources.sign_in_forgot_password
import orbit.shared.generated.resources.sign_in_google
import orbit.shared.generated.resources.sign_in_hide_password
import orbit.shared.generated.resources.sign_in_new
import orbit.shared.generated.resources.sign_in_password
import orbit.shared.generated.resources.sign_in_show_password
import orbit.shared.generated.resources.sign_in_subtitle
import orbit.shared.generated.resources.sign_in_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Sign-in, laid out as in the Sprint 1 mockup. Placeholders until their issues land: forgot
 * password, Google, Apple and create account are disabled, and there is no validation yet.
 */
@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val visibilityIcon = if (showPassword) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility
    val visibilityLabel = if (showPassword) Res.string.sign_in_hide_password else Res.string.sign_in_show_password
    val passwordShown = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
    OrbitScreen {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = OrbitSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                Text(stringResource(Res.string.sign_in_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(Res.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
                OrbitTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = stringResource(Res.string.sign_in_email),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_mail), contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                OrbitTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(Res.string.sign_in_password),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_lock), contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(painterResource(visibilityIcon), stringResource(visibilityLabel))
                        }
                    },
                    visualTransformation = passwordShown,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                TextButton(onClick = {}, modifier = Modifier.align(Alignment.End), enabled = false) {
                    Text(stringResource(Res.string.sign_in_forgot_password))
                }
                OrbitButton(
                    text = stringResource(Res.string.sign_in),
                    onClick = { viewModel.signIn(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
}
