package com.orbit.app.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlinx.coroutines.delay

/** How long after the one before it each tile starts to come in. */
private const val STAGGER_MILLIS = 40L
private const val TILE_RATIO = 0.8f

/**
 * A short code, one character per tile, such as an invitation's. Until there is a [code], [length] empty tiles hold
 * its place. A new code comes in tile by tile, as feedback that it was just made; screen readers hear it as one word.
 */
@Composable
fun OrbitCodeTiles(code: String?, length: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clearAndSetSemantics { code?.let { contentDescription = it } },
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
    ) {
        repeat(length) { index ->
            val character = code?.getOrNull(index)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(TILE_RATIO)
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                if (character != null) key(code) { Character(character, index) }
            }
        }
    }
}

@Composable
private fun Character(character: Char, index: Int) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * STAGGER_MILLIS)
        shown.animateTo(1f, OrbitMotion.spatial())
    }
    Text(
        text = character.toString(),
        modifier = Modifier.graphicsLayer {
            alpha = shown.value.coerceIn(0f, 1f)
            scaleX = shown.value
            scaleY = shown.value
        },
        style = OrbitTheme.data.large,
        textAlign = TextAlign.Center,
    )
}
