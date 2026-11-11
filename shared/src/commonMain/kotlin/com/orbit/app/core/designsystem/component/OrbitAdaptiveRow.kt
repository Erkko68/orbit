package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** Keep an action at the start and a value at the end, wrapping without squeezing either one. */
@Composable
fun OrbitAdaptiveRow(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Layout(content = { Box { leading() }; Box { trailing() } }, modifier = modifier) { children, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val action = children[0].measure(loose)
        val value = children[1].measure(loose)
        val gap = if (action.width == 0 || value.width == 0) 0 else OrbitSpacing.md.roundToPx()
        val width = constraints.maxWidth
        val stacked = action.width + gap + value.width > width
        val height = constraints.constrainHeight(
            if (stacked) action.height + gap + value.height else maxOf(
                action.height,
                value.height
            )
        )
        layout(width, height) {
            action.placeRelative(0, if (stacked) 0 else (height - action.height) / 2)
            value.placeRelative(width - value.width, if (stacked) action.height + gap else (height - value.height) / 2)
        }
    }
}
