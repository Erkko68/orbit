package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** Each option keeps Material's minimum touch target. */
private val SegmentHeight = 48.dp

/**
 * Switches between the sections of a screen, such as the items and the agenda of a space: a tonal track
 * with the selected option as a filled primary pill. Each option is a tab for accessibility services.
 */
@Composable
fun OrbitSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .background(scheme.surfaceContainer, CircleShape)
            .padding(OrbitSpacing.xs)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SegmentHeight)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else scheme.surfaceContainer)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
