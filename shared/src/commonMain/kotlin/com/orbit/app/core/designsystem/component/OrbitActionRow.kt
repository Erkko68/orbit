package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A row of an [OrbitGroup] that does something, such as changing a password: its [icon] on a [shape] in the space's
 * colour, [title] over [supporting], what it holds in [trailing], such as faces, and a way on. [danger] is for what
 * cannot be undone: the fill and the title go red.
 */
@Composable
fun OrbitActionRow(
    title: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    shape: OrbitShape = OrbitShape.Cookie,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    OrbitGroupRow(modifier, onClick = onClick) {
        OrbitShapeBadge(if (danger) scheme.error else OrbitTheme.accent.vivid, shape) {
            Icon(painterResource(icon), contentDescription = null, tint = if (danger) scheme.onError else Ink)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (danger) scheme.error else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.padding(end = OrbitSpacing.xs),
            tint = scheme.onSurfaceVariant,
        )
    }
}
