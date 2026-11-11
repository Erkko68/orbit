package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/** As big as an [OrbitShapeBadge], so a cluster and a badge line up in a list. */
private val ClusterSize = 52.dp

/**
 * A space as a panel, led by the [faces] of the people the user shares it with: the space is told apart by who is in
 * it, as a group chat without a photo is (#293). Until someone else joins, its [initials] on a [badge] fill in its
 * [shape] lead instead. Then its [name], how things stand in [line] and a smaller [detail].
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
        Row(
            Modifier.padding(OrbitSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (faces.isEmpty()) {
                OrbitShapeBadge(fill = badge, shape = shape) {
                    Text(initials, style = MaterialTheme.typography.titleMedium, color = Ink)
                }
            } else {
                FaceCluster(faces)
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
            }
        }
    }
}

/** Up to four faces in a square: one big, two across a diagonal, three in a triangle or four in a grid. */
@Composable
private fun FaceCluster(faces: List<OrbitFace>) {
    val shown = faces.take(4)
    val places = when (shown.size) {
        1 -> listOf(Alignment.Center)
        2 -> listOf(Alignment.TopStart, Alignment.BottomEnd)
        3 -> listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomCenter)
        else -> listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
    }
    val size = if (shown.size == 1) OrbitAvatarSize.Large else OrbitAvatarSize.Small
    Box(Modifier.size(ClusterSize)) {
        shown.zip(places).forEach { (face, place) ->
            OrbitAvatar(
                initials = face.initials,
                accent = face.color,
                modifier = Modifier.align(place),
                photoUrl = face.photoUrl,
                contentColor = face.contentColor,
                shape = face.shape,
                size = size,
            )
        }
    }
}
