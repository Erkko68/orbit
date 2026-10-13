package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.hide_password
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_visibility
import orbit.shared.generated.resources.ic_visibility_off
import orbit.shared.generated.resources.show_password
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * An [OrbitTextField] for a password: masked unless [visible], with a button that asks to show or
 * hide it through [onToggleVisibility]. Whether it is visible is the caller's state.
 */
@Composable
fun OrbitPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    OrbitTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        supportingText = supportingText,
        leadingIcon = { Icon(painterResource(Res.drawable.ic_lock), contentDescription = null) },
        trailingIcon = {
            OrbitIconButton(
                icon = if (visible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility,
                contentDescription = stringResource(if (visible) Res.string.hide_password else Res.string.show_password),
                onClick = onToggleVisibility,
            )
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
    )
}
