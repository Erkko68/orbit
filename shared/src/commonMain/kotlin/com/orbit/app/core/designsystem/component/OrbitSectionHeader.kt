package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/**
 * Opens a group of panels or rows on the page, such as "Lists": a bold [title] and, when set, a [count] ("3 open") and an [action], such as showing what is folded away. In a
 * sheet or a card it is smaller and sits closer to what it opens.
 */
@Composable
fun OrbitSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val inside = LocalInsideCard.current
    Row(
        modifier = modifier.padding(top = if (inside) OrbitSpacing.sm else OrbitSpacing.md, bottom = if (inside) 0.dp else OrbitSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = if (inside) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        count?.let {
            Text(it, style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        action?.let { OrbitTextButton(it, onAction) }
    }
}
