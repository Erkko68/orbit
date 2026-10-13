package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val AvatarSize = 36.dp
private val AvatarRing = 2.dp

/**
 * A member's photo, or their [initials] until it loads or when there is none, in a circle. [accent]
 * only rings it (decorative, usually `OrbitTheme.colors.spaceAccent(…)`).
 */
@Composable
fun OrbitAvatar(initials: String, accent: Color, modifier: Modifier = Modifier, photoUrl: String? = null) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .border(AvatarRing, accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
        if (photoUrl != null) {
            // Drawn over the initials, which show through while it loads or if it fails.
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize().padding(AvatarRing).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
