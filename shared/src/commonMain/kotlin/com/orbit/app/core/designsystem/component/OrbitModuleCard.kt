package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * One module of an item (schedule, assignment, checklist…): a header with the module's [icon] and
 * [title] and a button that calls [onRemove], then the module's [content]. [removeDescription] names
 * that button for screen readers, such as "Remove schedule". Tonal, like any card of secondary content.
 */
@Composable
fun OrbitModuleCard(
    title: String,
    icon: DrawableResource,
    removeDescription: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    removeEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    OrbitCard(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OrbitIconButton(Res.drawable.ic_close, removeDescription, onClick = onRemove, enabled = removeEnabled)
        }
        content()
    }
}
