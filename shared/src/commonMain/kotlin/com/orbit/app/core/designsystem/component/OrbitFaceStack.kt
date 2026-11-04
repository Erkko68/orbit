package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private const val MOST_FACES = 4
private val TouchTarget = 48.dp

/** A person as a face: their photo, or their [initials] on [color] in their [shape]. [name] says who it is. */
@Immutable
data class OrbitFace(
    val id: String,
    val name: String,
    val initials: String,
    val color: Color,
    val shape: OrbitShape,
    val photoUrl: String? = null,
    val contentColor: Color = Ink,
)

/**
 * Who is in something, standing still: up to four faces that overlap, and "+N" for the rest. With [onClick] the whole
 * stack is one button, named by [clickLabel].
 */
@Composable
fun OrbitFaceStack(
    faces: List<OrbitFace>,
    modifier: Modifier = Modifier,
    size: OrbitAvatarSize = OrbitAvatarSize.Small,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    val tappable = if (onClick == null) {
        modifier
    } else {
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .heightIn(min = TouchTarget)
            .padding(horizontal = OrbitSpacing.xs)
    }
    val more = faces.size - MOST_FACES
    Row(tappable, horizontalArrangement = Arrangement.spacedBy(-OrbitSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        for (face in faces.take(MOST_FACES)) {
            OrbitAvatar(
                initials = face.initials,
                accent = face.color,
                modifier = Modifier.semantics { contentDescription = face.name },
                photoUrl = face.photoUrl,
                contentColor = face.contentColor,
                shape = face.shape,
                size = size,
            )
        }
        if (more > 0) OrbitAvatar("+$more", Ink, contentColor = OrbitTheme.accent.vivid, size = size)
    }
}
