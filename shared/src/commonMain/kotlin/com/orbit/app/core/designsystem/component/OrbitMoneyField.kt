package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/** Room for the cursor after the last digit. */
private val CaretRoom = 2.dp

/**
 * The amount a form is about, such as what an expense cost: a panel of its own with its [label] over a big number in
 * the data type, the currency in [prefix] or [suffix] on the side the device writes it, quieter. [placeholder] stands in
 * while it is empty; while [isError] the number turns the error colour and [errorText] says why under it.
 */
@Composable
fun OrbitMoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    prefix: String? = null,
    suffix: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val number = OrbitTheme.data.display.copy(color = if (isError) scheme.error else scheme.onSurface)
    val quiet = number.copy(color = scheme.onSurfaceVariant)
    val field = rememberFieldValue(value)
    // The field hugs what it shows, so the currency sits right beside the number.
    val measurer = rememberTextMeasurer()
    val shown = field.value.text.ifEmpty { placeholder }
    val width = with(LocalDensity.current) { measurer.measure(shown, number).size.width.toDp() } + CaretRoom
    OrbitCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                prefix?.let { Text(it, style = quiet) }
                BasicTextField(
                    value = field.value,
                    onValueChange = {
                        field.value = it
                        onValueChange(it.text)
                    },
                    modifier = Modifier.width(width).semantics { contentDescription = label },
                    enabled = enabled,
                    textStyle = number.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(scheme.onSurface),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (field.value.text.isEmpty()) Text(placeholder, style = quiet)
                            inner()
                        }
                    },
                )
                suffix?.let { Text(it, style = quiet) }
            }
            if (isError && errorText != null) OrbitErrorText(errorText)
        }
    }
}
