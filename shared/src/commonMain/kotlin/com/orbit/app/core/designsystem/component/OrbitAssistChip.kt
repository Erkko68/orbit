package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A short action, such as adding a detail: a pill button, with no selected state. */
@Composable
fun OrbitAssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colors.surfaceContainer,
            labelColor = colors.onSurfaceVariant,
        ),
        border = null,
    )
}
