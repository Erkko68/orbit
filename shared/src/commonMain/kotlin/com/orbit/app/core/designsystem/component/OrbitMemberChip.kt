package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

private val ChipHeight = 48.dp

/**
 * A person in a row of chips, such as a member of a space: [leading] (their avatar) next to [name] over
 * [detail] (their role), on a tonal pill. [highlighted] puts it in the accent tone, for the current user or
 * the chip that invites someone. Tappable when [onClick] is set.
 */
@Composable
fun OrbitMemberChip(
    name: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (highlighted) scheme.primaryContainer else scheme.surfaceContainer
    val content = if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
    val secondary = if (highlighted) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .heightIn(min = ChipHeight)
                .padding(start = OrbitSpacing.xs, end = OrbitSpacing.md)
                .padding(vertical = OrbitSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column {
                Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = secondary, maxLines = 1) }
            }
        }
    }
    if (onClick == null) {
        Surface(modifier, shape = CircleShape, color = container, contentColor = content, content = body)
    } else {
        Surface(onClick, modifier, shape = CircleShape, color = container, contentColor = content, content = body)
    }
}
