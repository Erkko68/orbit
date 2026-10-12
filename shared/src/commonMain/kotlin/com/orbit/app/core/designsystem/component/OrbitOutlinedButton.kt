package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** Secondary action, such as "Join space" or a sign-in provider ([icon] is its logo): an outlined pill. */
@Composable
fun OrbitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceBright,
            contentColor = colors.onSurface,
        ),
        // M3 strokes with outlineVariant, which is the dark card fill: outline stays visible in both schemes.
        border = BorderStroke(1.dp, colors.outline),
    ) {
        if (icon != null) {
            Box(Modifier.size(ButtonDefaults.IconSize)) { icon() }
            Spacer(Modifier.width(OrbitSpacing.md))
        }
        Text(text)
    }
}
