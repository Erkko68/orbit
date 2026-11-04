package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private const val TITLE_LINES = 2

/**
 * An item as a row of an [OrbitGroup], the same for a task and a list: the [OrbitCheck] first, then the [title], a line
 * of [summary] and [label] (when it is due, how far along a list is) one under the other, and the [faces] of who has it
 * at the end of the row.
 * [checkDescription] says what the check does for screen readers. A row that is [checked] is tinted and its title goes
 * quiet.
 */
@Composable
fun OrbitTaskRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    label: String? = null,
    faces: List<OrbitFace> = emptyList(),
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    OrbitGroupRow(modifier, onClick = onClick, done = checked) {
        OrbitCheck(checked, onCheckedChange, checkDescription, enabled = enabled, compact = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            label?.let { Text(it, style = OrbitTheme.data.small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (faces.isNotEmpty()) OrbitFaceStack(faces)
    }
}
