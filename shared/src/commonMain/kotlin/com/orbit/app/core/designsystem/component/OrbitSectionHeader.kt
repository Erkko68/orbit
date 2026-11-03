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
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/** Opens a group of panels or rows on the page, such as "Lists": a bold [title] and, when set, a [count] ("3 open"). */
@Composable
fun OrbitSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    Row(
        modifier = modifier.padding(top = OrbitSpacing.md, bottom = OrbitSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        count?.let {
            Text(it, style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
