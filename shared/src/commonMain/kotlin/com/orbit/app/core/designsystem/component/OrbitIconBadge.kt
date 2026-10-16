package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val BadgeSize = 40.dp

/**
 * A primary icon on a circle, leading a card's name (a list's cart, an item's type) or as its arrow. The
 * circle takes its fill from the card it sits in. Decorative: the card's text says what it is.
 */
@Composable
fun OrbitIconBadge(icon: DrawableResource, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(BadgeSize).background(insetFill(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}
