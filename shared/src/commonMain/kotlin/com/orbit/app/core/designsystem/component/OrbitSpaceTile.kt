package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val PlanetSize = 160.dp
private val PlanetBleed = 48.dp

/**
 * A space as a card of its own [color], with a still [shape] in its [partner] colour running off its top right corner: its [name],
 * how things stand in [line], a smaller [detail], then the [faces] of its members. Text on it is ink.
 */
@Composable
fun OrbitSpaceTile(
    name: String,
    line: String,
    color: Color,
    partner: Color,
    shape: OrbitShape,
    faces: List<OrbitFace>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = Ink,
    ) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = PlanetBleed, y = -PlanetBleed)
                    .size(PlanetSize)
                    .background(partner, shape),
            )
            Column(Modifier.padding(OrbitSpacing.md), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                Text(name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let { Text(it, style = OrbitTheme.data.small, color = InkSoft, maxLines = 1) }
                if (faces.isNotEmpty()) OrbitFaceStack(faces, Modifier.padding(top = OrbitSpacing.sm))
            }
        }
    }
}
