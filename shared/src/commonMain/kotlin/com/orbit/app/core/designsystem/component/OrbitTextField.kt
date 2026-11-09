package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Single-line input: a sunk fill with no stroke until it is focused (ink) or in error. */
@Composable
fun OrbitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: DrawableResource? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: String? = null,
    suffix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val fill = colors.surfaceContainer
    val field = rememberFieldValue(value)
    OutlinedTextField(
        value = field.value,
        onValueChange = {
            field.value = it
            onValueChange(it.text)
        },
        modifier = modifier.heightIn(min = ControlHeight),
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = leadingIcon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingIcon = trailingIcon,
        prefix = prefix?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            disabledContainerColor = fill,
            errorContainerColor = fill,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
        ),
    )
}
