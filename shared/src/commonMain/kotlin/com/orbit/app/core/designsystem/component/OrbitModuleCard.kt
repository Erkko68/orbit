package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_close
import orbit.shared.generated.resources.module_card_remove
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ModuleIconSize = 24.dp

/**
 * One module of an item (schedule, assignment, checklist…): a header with the module's [icon],
 * its [title] and a button that calls [onRemove], then the module's [content].
 */
@Composable
fun OrbitModuleCard(
    title: String,
    icon: @Composable () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OrbitCard(modifier = modifier) {
        // Icons are secondary content; the title keeps the main text color.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(ModuleIconSize)) { icon() }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onRemove) {
                    Icon(painterResource(Res.drawable.ic_close), stringResource(Res.string.module_card_remove, title))
                }
            }
        }
        content()
    }
}
