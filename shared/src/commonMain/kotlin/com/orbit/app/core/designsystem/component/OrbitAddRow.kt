package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource

private val AddSize = 36.dp

/**
 * The row of an [OrbitGroup] that adds to it, typed in place: the round button and the keyboard's Done both call [onAdd],
 * which [placeholder] names, and the cursor stays for the next one. What is typed is the caller's state.
 */
@Composable
fun OrbitAddRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    canAdd: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val field = rememberFieldValue(value)
    OrbitGroupRow(modifier) {
        FilledIconButton(
            onClick = onAdd,
            modifier = Modifier.size(AddSize),
            enabled = canAdd,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Ink,
                contentColor = OrbitTheme.accent.vivid,
                disabledContainerColor = Ink,
                disabledContentColor = OrbitTheme.accent.vivid,
            ),
        ) {
            Icon(painterResource(Res.drawable.ic_add), placeholder)
        }
        BasicTextField(
            value = field.value,
            onValueChange = {
                field.value = it
                onValueChange(it.text)
            },
            modifier = Modifier.weight(1f).semantics { contentDescription = placeholder },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.onSurface),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd() }),
            decorationBox = { inner ->
                Box {
                    if (field.value.text.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    }
                    inner()
                }
            },
        )
    }
}
