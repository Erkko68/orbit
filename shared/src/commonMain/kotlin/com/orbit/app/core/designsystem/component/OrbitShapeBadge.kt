package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitShape

private val BadgeSize = 52.dp

/** A [fill] in one of the expressive shapes, with an icon or the mark in the middle. It leads a row or opens a tile. */
@Composable
fun OrbitShapeBadge(
    fill: Color,
    shape: OrbitShape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.size(BadgeSize).background(fill, shape), contentAlignment = Alignment.Center, content = content)
}
