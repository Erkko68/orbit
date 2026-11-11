package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val FieldWidth = OrbitSpacing.xl * 3
private val FieldHeight = 48.dp
private val ErrorStroke = 2.dp

/**
 * An amount typed at the end of an [OrbitGroupRow], such as one person's share: right-aligned in the data type, on a
 * sunk fill, with a red edge while [isError]. [description] names it for screen readers; [placeholder] stands in while
 * it is empty. [prefix] or [suffix] is the currency, on the side the device writes it.
 */
@Composable
fun OrbitAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    prefix: String? = null,
    suffix: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val style = OrbitTheme.data.medium.copy(color = scheme.onSurface, textAlign = TextAlign.End)
    val field = rememberFieldValue(value)
    val shape = MaterialTheme.shapes.medium
    val parts = listOfNotNull(prefix, value.ifEmpty { placeholder }, suffix)
    val measured = rememberTextMeasurer().measure(parts.joinToString(""), style, softWrap = false, maxLines = 1)
    val density = LocalDensity.current
    val width = maxOf(
        FieldWidth,
        with(density) { measured.size.width.toDp() } + OrbitSpacing.sm * 2 + OrbitSpacing.xs * (parts.size - 1))
    val height = maxOf(FieldHeight, with(density) { measured.size.height.toDp() } + OrbitSpacing.sm * 2)
    BasicTextField(
        value = field.value,
        onValueChange = {
            field.value = it
            onValueChange(it.text)
        },
        modifier = modifier.width(width).height(height).semantics { contentDescription = description },
        enabled = enabled,
        textStyle = style,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(scheme.onSurface),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .background(scheme.surfaceContainer, shape)
                    .then(if (isError) Modifier.border(ErrorStroke, scheme.error, shape) else Modifier)
                    .padding(horizontal = OrbitSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                prefix?.let { Text(it, style = style) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (field.value.text.isEmpty()) {
                        Text(placeholder, style = style.copy(color = scheme.onSurfaceVariant))
                    }
                    inner()
                }
                suffix?.let { Text(it, style = style) }
            }
        },
    )
}
