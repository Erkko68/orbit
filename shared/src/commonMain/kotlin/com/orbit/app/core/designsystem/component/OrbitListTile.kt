package com.orbit.app.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.InkSoft
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val BigCorner = 32.dp
private val TightCorner = 10.dp
private val CornerSize = 96.dp
private val CornerBleed = 32.dp
private val FaceStage = 64.dp
private val LargestFace = 16.dp
private val TitleClearance = 52.dp
private const val PEEK_LINES = 2

/** Where a tile sits in its row: tiles that meet have tight corners, so a pair reads as one thing. */
enum class OrbitListTileSlot { Start, End, Alone }

/**
 * A list as a solid tile, as tall as what it has to say: its [title], what is left as [peek] with a [peekNote] after it,
 * and how far along it is, as [count] and [progress]. A shape in the other fill runs off the corner with [faces] floating
 * on it. The fill is the space's vivid colour, or its partner for the [OrbitListTileSlot.End] slot. Next to a taller tile
 * it stretches, with the progress at the bottom. Text on it is always ink.
 */
@Composable
fun OrbitListTile(
    title: String,
    count: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    peek: List<String> = emptyList(),
    peekNote: String? = null,
    faces: List<OrbitFloatingFace> = emptyList(),
    slot: OrbitListTileSlot = OrbitListTileSlot.Alone,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inner by animateDpAsState(if (pressed) BigCorner else TightCorner, OrbitMotion.spatial())
    val accent = OrbitTheme.accent
    val end = slot == OrbitListTileSlot.End
    val (fill, shape) = when (slot) {
        OrbitListTileSlot.Start -> accent.vivid to RoundedCornerShape(BigCorner, inner, inner, BigCorner)
        OrbitListTileSlot.End -> accent.partner to RoundedCornerShape(inner, BigCorner, BigCorner, inner)
        OrbitListTileSlot.Alone -> accent.vivid to RoundedCornerShape(BigCorner)
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = fill,
        contentColor = Ink,
        interactionSource = interaction,
    ) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = CornerBleed, y = -CornerBleed)
                    .size(CornerSize)
                    .background(if (end) accent.vivid else accent.partner, if (end) OrbitShape.Clover else OrbitShape.Cookie),
            )
            OrbitFloatingFaces(faces, Modifier.align(Alignment.TopEnd).size(FaceStage), largest = LargestFace)
            Column(Modifier.fillMaxHeight().padding(OrbitSpacing.md)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(end = TitleClearance),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val left = (peek + listOfNotNull(peekNote)).joinToString()
                if (left.isNotEmpty()) {
                    Text(
                        text = left,
                        modifier = Modifier.padding(top = OrbitSpacing.xs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        maxLines = PEEK_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.padding(top = OrbitSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OrbitWavyProgress(progress, Modifier.weight(1f))
                    Text(count, style = OrbitTheme.data.small, maxLines = 1)
                }
            }
        }
    }
}
