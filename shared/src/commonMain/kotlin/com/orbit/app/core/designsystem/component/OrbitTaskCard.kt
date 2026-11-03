package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private const val MOST_FACES = 4
private const val TITLE_LINES = 2

/**
 * A task as a card of its own, where the [title] does the talking: it always comes first, so a card with nothing else is
 * a clean line of big type. Under it, when there is any, a line of [summary], then a footer with [label] (when it is due)
 * and the [faces] of who has it. The [OrbitCheck] stands on the right, and [checkDescription] says what it does for screen
 * readers. A card that is done is tinted and its title goes quiet.
 */
@Composable
fun OrbitTaskCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    label: String? = null,
    faces: List<OrbitFloatingFace> = emptyList(),
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val fill by animateColorAsState(if (checked) OrbitTheme.accent.done else scheme.surfaceBright, OrbitMotion.effects())
    Surface(
        onClick = onClick,
        modifier = modifier.panelShadow(shape),
        shape = shape,
        color = fill,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = OrbitSpacing.md, end = OrbitSpacing.xs).padding(vertical = OrbitSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (checked) scheme.onSurfaceVariant else scheme.onSurface,
                    maxLines = TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (label != null || faces.isNotEmpty()) TaskFooter(label, faces)
            }
            OrbitCheck(checked, onCheckedChange, checkDescription, enabled = enabled)
        }
    }
}

@Composable
private fun TaskFooter(label: String?, faces: List<OrbitFloatingFace>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = OrbitSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.orEmpty(), Modifier.weight(1f), style = OrbitTheme.data.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(-OrbitSpacing.xs)) {
            for (face in faces.take(MOST_FACES)) {
                OrbitAvatar(
                    initials = face.initials,
                    accent = face.color,
                    modifier = Modifier.semantics { contentDescription = face.description },
                    photoUrl = face.photoUrl,
                    contentColor = face.contentColor,
                    shape = face.shape,
                    size = OrbitAvatarSize.Small,
                )
            }
        }
    }
}
