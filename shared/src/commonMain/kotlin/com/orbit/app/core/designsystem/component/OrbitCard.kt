package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** True inside an [OrbitCard], so components on it can stand out from the card fill (see [OrbitTextField]). */
internal val LocalInsideCard = staticCompositionLocalOf { false }

/** True inside an elevated [OrbitCard] or a dialog, whose fill is the bright one. */
internal val LocalCardElevated = staticCompositionLocalOf { false }

/**
 * The fill of something set into its container, such as a row or a badge: the tonal fill on a bright
 * card, the brightest fill on a tonal card (darker in dark, so it reads as recessed), the card fill on the canvas.
 */
@Composable
internal fun insetFill(): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        LocalCardElevated.current -> scheme.surfaceContainerLow
        LocalInsideCard.current -> scheme.surfaceContainerLowest
        else -> scheme.surfaceContainer
    }
}

private val ElevatedCardElevation = 4.dp

/**
 * Container for a group of content, with 24dp corners. Tonal by default: the card fill and no shadow,
 * for secondary content and groups. [elevated] lifts it off the canvas with a bright fill and a soft
 * shadow: use it for the main content of a screen, such as the cards of a list. Tappable when
 * [onClick] is set.
 */
@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevated: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.cardColors(containerColor = if (elevated) scheme.surfaceBright else scheme.surfaceContainer)
    val elevation = CardDefaults.cardElevation(defaultElevation = if (elevated) ElevatedCardElevation else 0.dp)
    val body: @Composable ColumnScope.() -> Unit = {
        CompositionLocalProvider(LocalInsideCard provides true, LocalCardElevated provides elevated) {
            Column(Modifier.padding(OrbitSpacing.md), verticalArrangement = verticalArrangement, content = content)
        }
    }
    if (onClick == null) {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = body)
    } else {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = body)
    }
}
