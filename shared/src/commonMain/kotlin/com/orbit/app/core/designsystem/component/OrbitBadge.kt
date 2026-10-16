package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** A non-interactive pill for a type or status, with the theme's accessible container colours. */
@Composable
fun OrbitBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.xs),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
