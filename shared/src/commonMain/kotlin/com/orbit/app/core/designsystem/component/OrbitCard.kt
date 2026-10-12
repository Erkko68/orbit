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
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** True inside an [OrbitCard], so components on it can stand out from the card fill (see [OrbitTextField]). */
internal val LocalInsideCard = staticCompositionLocalOf { false }

/** Tonal container for a group of content: card fill, 16dp corners, no shadow. Tappable when [onClick] is set. */
@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val body: @Composable ColumnScope.() -> Unit = {
        CompositionLocalProvider(LocalInsideCard provides true) {
            Column(Modifier.padding(OrbitSpacing.md), verticalArrangement = verticalArrangement, content = content)
        }
    }
    if (onClick == null) {
        Card(modifier = modifier, colors = colors, content = body)
    } else {
        Card(onClick = onClick, modifier = modifier, colors = colors, content = body)
    }
}
