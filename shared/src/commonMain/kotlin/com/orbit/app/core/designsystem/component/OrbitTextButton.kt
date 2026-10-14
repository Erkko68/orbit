package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A secondary text action with the same height and pill shape as the other Orbit controls. */
@Composable
fun OrbitTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ControlHeight),
        enabled = enabled,
        shape = CircleShape,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
