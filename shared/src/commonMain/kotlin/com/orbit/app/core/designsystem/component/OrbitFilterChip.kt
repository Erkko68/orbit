package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ChipHeight = 40.dp
private val SelectedElevation = 2.dp

/**
 * One option of a filter row, such as "Tasks" over a list of items. On the page it is plain text, and the selected one
 * sits on a soft panel. On a panel, where that would not show, it is a sunk fill and the selected one is ink. [icon]
 * goes before the label.
 */
@Composable
fun OrbitFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: DrawableResource? = null,
) {
    val colors = MaterialTheme.colorScheme
    val onPanel = LocalInsideCard.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = icon?.let {
            { Icon(painterResource(it), contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        },
        modifier = modifier.heightIn(min = ChipHeight),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (onPanel) colors.surfaceContainer else Color.Transparent,
            labelColor = colors.onSurfaceVariant,
            iconColor = colors.onSurfaceVariant,
            selectedLeadingIconColor = if (onPanel) colors.onPrimary else colors.onSurface,
            selectedContainerColor = if (onPanel) colors.primary else colors.surfaceBright,
            selectedLabelColor = if (onPanel) colors.onPrimary else colors.onSurface,
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = SelectedElevation,
        ),
        border = null,
    )
}
