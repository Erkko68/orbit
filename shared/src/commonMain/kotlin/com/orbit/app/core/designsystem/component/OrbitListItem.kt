package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * One row of a list, such as a space, an item or a member: [title] over a line of [supporting] text,
 * after an optional [leading] avatar or icon and before an optional [trailing] one. It has no fill of
 * its own: put it in an [OrbitCard] to make a card of it.
 */
@Composable
fun OrbitListItem(
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleDecoration: TextDecoration = TextDecoration.None,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = titleDecoration,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}
