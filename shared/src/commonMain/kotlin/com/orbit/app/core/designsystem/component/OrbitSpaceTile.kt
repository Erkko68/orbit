package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.InkSoft
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val TileHeight = 172.dp
private val PlanetSize = 190.dp
private val PlanetBleed = 38.dp
private val FacesWidth = 160.dp
private val LargestFace = 36.dp
private const val TEXT_WIDTH = 0.58f

/**
 * A place in an `OrbitGroup` of its own [color]: a [shape] in its [partner] colour runs off the edge with [faces]
 * floating on it, beside its big [name], how things stand in [line] and a smaller [detail]. Text on it is ink.
 */
@Composable
fun OrbitSpaceTile(
    name: String,
    line: String,
    color: Color,
    partner: Color,
    shape: OrbitShape,
    faces: List<OrbitFloatingFace>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(TileHeight),
        shape = groupShape(interaction),
        color = color,
        contentColor = Ink,
        interactionSource = interaction,
    ) {
        Box {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = PlanetBleed)
                    .size(PlanetSize)
                    .background(partner, shape),
            )
            OrbitFloatingFaces(
                faces = faces,
                modifier = Modifier.align(Alignment.CenterEnd).size(width = FacesWidth, height = TileHeight),
                largest = LargestFace,
            )
            Column(
                modifier = Modifier.fillMaxWidth(TEXT_WIDTH).fillMaxHeight().padding(OrbitSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                Text(name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Text(line, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let { Text(it, style = OrbitTheme.data.small, color = InkSoft, maxLines = 1) }
            }
        }
    }
}
