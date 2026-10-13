package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Height of every Orbit button and text field, so a form reads as one rhythm: the M3 text field
 * height, which is also the medium button of M3 Expressive and the extended FAB.
 */
internal val ControlHeight = 56.dp

/** Main action of a screen or card: a filled pill in the primary color. */
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = ControlHeight), enabled = enabled) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
