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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/** True inside a panel or a dialog, where a chip cannot be selected by being brighter than what it sits on. */
internal val LocalInsideCard = staticCompositionLocalOf { false }

private val PanelElevation = 2.dp
private const val AMBIENT_ALPHA = 0.08f
private const val SPOT_ALPHA = 0.16f

/** The shadow you only just see: what lifts a panel, a control or the bar off the page, with no outline. */
@Composable
internal fun Modifier.panelShadow(shape: Shape): Modifier {
    val shadow = OrbitTheme.colors.shadow
    return shadow(
        elevation = PanelElevation,
        shape = shape,
        ambientColor = shadow.copy(alpha = AMBIENT_ALPHA),
        spotColor = shadow.copy(alpha = SPOT_ALPHA),
    )
}

/** A panel: a soft fill with 24dp corners and no outline, on the page's tint. */
@Composable
fun OrbitCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = modifier.panelShadow(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
    ) {
        CompositionLocalProvider(LocalInsideCard provides true) {
            Column(
                modifier = Modifier.padding(OrbitSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                content = content,
            )
        }
    }
}
