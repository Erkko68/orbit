package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** Secondary action on a sunk fill. [compact] uses a smaller label and insets while keeping the same touch height. */
@Composable
fun OrbitTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ControlHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = if (compact) PaddingValues(OrbitSpacing.sm) else ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceContainer,
            contentColor = colors.onSurface,
        ),
    ) {
        Text(text, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall)
    }
}
