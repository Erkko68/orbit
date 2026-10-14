package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.orbit.app.core.designsystem.component.OrbitTextField
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_mail
import orbit.shared.generated.resources.sign_in_email
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The email field of sign-in and create account, with the [error] under it when there is one. */
@Composable
internal fun AuthEmailField(value: String, onValueChange: (String) -> Unit, error: StringResource?) {
    OrbitTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(Res.string.sign_in_email),
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { stringResource(it) },
        leadingIcon = Res.drawable.ic_mail,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
}
