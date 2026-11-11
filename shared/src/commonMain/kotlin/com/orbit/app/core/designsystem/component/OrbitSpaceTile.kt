package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/**
 * A space as a panel, opened by its badge: its [initials] on a [badge] fill in its [shape], which tells it apart from the
 * others without reading the name or relying on colour (#293). Then its [name], how things stand in [line], a smaller
 * [detail] and the [faces] of its members.
 */
@Composable
fun OrbitSpaceTile(
    name: String,
    initials: String,
    line: String,
    badge: Color,
    shape: OrbitShape,
    faces: List<OrbitFace>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(OrbitSpacing.md), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitShapeBadge(fill = badge, shape = shape) {
                Text(initials, style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                Text(name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                detail?.let {
                    Text(it, style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (faces.isNotEmpty()) OrbitFaceStack(faces, Modifier.padding(top = OrbitSpacing.sm))
            }
        }
    }
}
