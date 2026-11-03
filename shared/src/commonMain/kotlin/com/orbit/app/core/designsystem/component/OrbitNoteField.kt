package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * Text of several lines typed where it is read, such as an item's description: on an [OrbitGroupRow] it reads as text, not
 * as a form, with [placeholder] until there is some. Leaving the field calls [onDone]. On a vivid fill, pass [color] and
 * [placeholderColor] for it.
 */
@Composable
fun OrbitNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
) {
    val field = rememberFieldValue(value)
    BasicTextField(
        value = field.value,
        onValueChange = {
            field.value = it
            onValueChange(it.text)
        },
        modifier = modifier
            .onFocusChanged { if (!it.isFocused) onDone() }
            .semantics { contentDescription = placeholder },
        enabled = enabled,
        textStyle = style.copy(color = color),
        cursorBrush = SolidColor(color),
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box {
                if (field.value.text.isEmpty()) {
                    Text(placeholder, style = style, color = placeholderColor)
                }
                inner()
            }
        },
    )
}
