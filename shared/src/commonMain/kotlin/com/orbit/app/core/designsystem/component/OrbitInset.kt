package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * A row set into a card: an entry of a group card (a checklist entry) or the strip at the bottom of an
 * elevated card (who, and when). It takes its fill from the card it sits in, so the two always differ.
 * Tappable when [onClick] is set.
 */
@Composable
fun OrbitInset(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
    val shape = MaterialTheme.shapes.small
    val color = insetFill()
    val contentColor = MaterialTheme.colorScheme.onSurface
    if (onClick == null) {
        Surface(modifier, shape = shape, color = color, contentColor = contentColor, content = body)
    } else {
        Surface(onClick, modifier, shape = shape, color = color, contentColor = contentColor, content = body)
    }
}
