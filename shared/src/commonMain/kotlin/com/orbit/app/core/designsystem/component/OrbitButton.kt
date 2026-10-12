package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Touch target of every Orbit button. M3 buttons are 40dp tall by default. */
internal val ButtonMinHeight = 48.dp

/** Main action of a screen or card: a filled pill in the primary color. */
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = ButtonMinHeight), enabled = enabled) {
        Text(text)
    }
}
