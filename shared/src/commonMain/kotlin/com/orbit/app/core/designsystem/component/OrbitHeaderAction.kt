package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ActionHeight = 48.dp
private const val DISABLED_ALPHA = 0.45f

/**
 * The one action of an [OrbitColorHeader]: a pill with its [icon] and [text], in ink, or in the space's partner colour
 * when [quiet], once it has been done and only undoes. It fades while it cannot be used.
 */
@Composable
fun OrbitHeaderAction(
    text: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    quiet: Boolean = false,
) {
    val accent = OrbitTheme.accent
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        enabled = enabled,
        shape = CircleShape,
        color = if (quiet) accent.partner else Ink,
        contentColor = if (quiet) Ink else accent.vivid,
    ) {
        Row(
            modifier = Modifier.heightIn(min = ActionHeight).padding(horizontal = OrbitSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null)
            Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
    }
}
