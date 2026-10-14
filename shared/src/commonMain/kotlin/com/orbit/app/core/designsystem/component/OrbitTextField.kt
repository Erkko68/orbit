package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Single-line input: a tonal fill with no stroke until it is focused (primary) or in error.
 * On an [OrbitCard] the card already has that fill, so the field takes the canvas color and an outline.
 */
@Composable
fun OrbitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: String? = null,
    readOnly: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val insideCard = LocalInsideCard.current
    val fill = if (insideCard) colors.surface else colors.surfaceContainer
    val restingBorder = if (insideCard) colors.outline else Color.Transparent
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = ControlHeight),
        enabled = enabled,
        placeholder = { Text(placeholder) },
        label = label?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        readOnly = readOnly,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            disabledContainerColor = fill,
            errorContainerColor = fill,
            unfocusedBorderColor = restingBorder,
            disabledBorderColor = restingBorder,
        ),
    )
}
