package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Keep the three values aligned; give a long name its own line when the complete sentence will not fit. */
@Composable
fun OrbitBalanceRow(
    name: String,
    status: String,
    amount: String,
    direction: DrawableResource?,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val statusStyle =
        if (direction == null) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall
    val statusWidth = textMeasurer.measure(status, statusStyle).size.width
    val gap = OrbitSpacing.sm
    OrbitGroupRow(modifier.semantics(mergeDescendants = true) {}) {
        Layout(
            modifier = Modifier.weight(1f).padding(end = OrbitSpacing.sm).padding(vertical = OrbitSpacing.xs),
            content = {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (direction == null) {
                    Text(status, style = statusStyle, textAlign = TextAlign.Center)
                } else {
                    BalanceDirection(status, direction, Modifier.fillMaxWidth())
                }
                Text(amount, style = OrbitTheme.data.large, textAlign = TextAlign.End, softWrap = false)
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val value = measurables[2].measure(loose)
            val columnWidth = ((width - value.width - 2 * gap.roundToPx()) / 2).coerceAtLeast(0)
            val indicatorWidth = maxOf(statusWidth, OrbitSpacing.lg.roundToPx())
            val inline =
                columnWidth >= measurables[0].maxIntrinsicWidth(Constraints.Infinity) && columnWidth >= indicatorWidth
            val directionWidth = (if (inline) columnWidth else indicatorWidth).coerceAtMost(width)
            val indicator = measurables[1].measure(loose.copy(minWidth = directionWidth, maxWidth = directionWidth))
            val person = measurables[0].measure(loose.copy(maxWidth = if (inline) columnWidth else width))
            val lineHeight = maxOf(indicator.height, value.height, if (inline) person.height else 0)
            val lineTop = if (inline) 0 else person.height + gap.roundToPx()
            layout(width, constraints.constrainHeight(lineTop + lineHeight)) {
                person.placeRelative(0, if (inline) (lineHeight - person.height) / 2 else 0)
                value.placeRelative(width - value.width, lineTop + (lineHeight - value.height) / 2)
                val indicatorLeft = if (inline) columnWidth + gap.roundToPx() else maxOf(
                    0,
                    (width - value.width - indicator.width - gap.roundToPx()) / 2
                )
                indicator.placeRelative(indicatorLeft, lineTop + (lineHeight - indicator.height) / 2)
            }
        }
    }
}

/** The label grows above the arrow while the arrow stays on the name/amount centre line, even at large font sizes. */
@Composable
private fun BalanceDirection(status: String, direction: DrawableResource, modifier: Modifier) {
    val gap = OrbitSpacing.xs
    Layout(
        content = {
            Text(status, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            Icon(painterResource(direction), contentDescription = null)
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val label = measurables[0].measure(loose)
        val arrow = measurables[1].measure(loose)
        val width = constraints.maxWidth
        val height = constraints.constrainHeight(arrow.height + 2 * (label.height + gap.roundToPx()))
        layout(width, height) {
            val arrowTop = (height - arrow.height) / 2
            arrow.placeRelative((width - arrow.width) / 2, arrowTop)
            label.placeRelative((width - label.width) / 2, arrowTop - gap.roundToPx() - label.height)
        }
    }
}
