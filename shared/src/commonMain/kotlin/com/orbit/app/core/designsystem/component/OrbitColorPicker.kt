package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** The touch target of one colour, like every control. The colour sits inside it, so a ring fits around it. */
private val SwatchTarget = 48.dp
private val SwatchRing = 3.dp
private val SwatchInset = 6.dp

/**
 * A row of [colors] to pick one from, such as the accent of a space. [labels] name the colours for screen
 * readers, one for each, and the colour at [selectedIndex] is ringed. Which one is selected is the caller's
 * state: [onSelect] runs with the index of the one tapped.
 */
@Composable
fun OrbitColorPicker(
    colors: List<Color>,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(colors.size == labels.size) { "Each colour needs a label" }
    val ring = MaterialTheme.colorScheme.onSurface
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        colors.forEachIndexed { index, color ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .size(SwatchTarget)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.RadioButton)
                    .semantics { contentDescription = labels[index] }
                    .then(if (selected) Modifier.border(SwatchRing, ring, CircleShape) else Modifier)
                    .padding(SwatchInset)
                    .background(color, CircleShape),
            )
        }
    }
}
