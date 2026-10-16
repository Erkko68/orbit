package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val PillIconSize = 16.dp

/** What a pill says about its label: plain meta, the one that matters, a good state, or one that needs attention. */
enum class OrbitPillTone { Neutral, Accent, Success, Warning }

/**
 * A short, non-interactive label on a full pill, such as an item's type, a count or a status. Used in the
 * line above a card's name and next to a section header. Every tone pairs a container with its on-color.
 */
@Composable
fun OrbitPill(
    label: String,
    modifier: Modifier = Modifier,
    tone: OrbitPillTone = OrbitPillTone.Neutral,
    icon: DrawableResource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        OrbitPillTone.Neutral -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        OrbitPillTone.Accent -> scheme.primaryContainer to scheme.onPrimaryContainer
        OrbitPillTone.Success -> scheme.secondaryContainer to scheme.onSecondaryContainer
        OrbitPillTone.Warning -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = OrbitSpacing.sm, vertical = OrbitSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(PillIconSize)) }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
