package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing

private val SegmentWidth = 56.dp
private val TouchHeight = 48.dp
private val PillHeight = 40.dp

/** Compact two-way choice. Only the sliding indicator owns animation state; the caller owns selection. */
@Composable
fun OrbitUnitSelector(
    firstLabel: String,
    secondLabel: String,
    firstDescription: String,
    secondDescription: String,
    secondSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val position by animateDpAsState(if (secondSelected) SegmentWidth else 0.dp, OrbitMotion.spatial())
    Box(
        modifier.width(SegmentWidth * 2).height(TouchHeight).selectableGroup(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(PillHeight).background(scheme.surfaceContainer, CircleShape))
        Box(
            Modifier.offset(x = position).width(SegmentWidth).height(PillHeight)
                .padding(OrbitSpacing.xs).background(scheme.primary, CircleShape),
        )
        Row(Modifier.fillMaxSize()) {
            listOf(firstLabel to firstDescription, secondLabel to secondDescription).forEachIndexed { index, option ->
                val selected = secondSelected == (index == 1)
                val color by animateColorAsState(
                    if (selected) scheme.onPrimary else scheme.onSurface,
                    OrbitMotion.effects()
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(
                            selected,
                            interactionSource = null,
                            indication = null,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onSelect(index == 1) },
                        )
                        .semantics { contentDescription = option.second },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option.first, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
    }
}
