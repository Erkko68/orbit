package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val EntryHeight = 56.dp
private val StrikeWidth = 1.5.dp
private const val ENTRY_LINES = 2

/**
 * One entry of a checklist in an [OrbitGroup]: lighter than a full row, so a long list reads as a list. The check
 * comes first, then [text] and its [quantity]. Ticking it draws a line through the text and tints the row like any
 * done row: it stays a panel, readable, and steps back. Tapping the row calls [onClick]. [checkDescription] says what
 * the check does for screen readers.
 */
@Composable
fun OrbitEntryRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quantity: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(if (checked) OrbitTheme.accent.done else scheme.surfaceBright, OrbitMotion.effects())
    val ink by animateColorAsState(if (checked) scheme.onSurfaceVariant else scheme.onSurface, OrbitMotion.effects())
    val strike = animateFloatAsState(if (checked) 1f else 0f, OrbitMotion.effects())
    var lines by remember { mutableStateOf<TextLayoutResult?>(null) }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = groupShape(interaction),
        color = fill,
        contentColor = ink,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.heightIn(min = EntryHeight).padding(end = OrbitSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitCheck(checked, onCheckedChange, checkDescription, compact = true)
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = OrbitSpacing.sm)
                    .drawWithContent {
                        drawContent()
                        val layout = lines ?: return@drawWithContent
                        for (line in 0 until layout.lineCount) {
                            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
                            val start = layout.getLineLeft(line)
                            val end = start + (layout.getLineRight(line) - start) * strike.value
                            if (end > start) drawLine(ink, Offset(start, y), Offset(end, y), StrikeWidth.toPx())
                        }
                    },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = ENTRY_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { lines = it },
            )
            quantity?.let { Text(it, style = OrbitTheme.data.small, maxLines = 1) }
        }
    }
}
