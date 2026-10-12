package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AvatarSize = 36.dp
private val AvatarRing = 2.dp

/** A member's [initials] in a circle. [accent] only rings it (decorative, usually a space accent). */
@Composable
fun OrbitAvatar(initials: String, accent: Color, modifier: Modifier = Modifier) {
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
    }
}
