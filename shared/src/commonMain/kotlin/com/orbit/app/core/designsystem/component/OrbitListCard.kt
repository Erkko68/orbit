package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * An [OrbitListItem] on its own tonal card, such as an item of a space or an action of the account
 * screen: an [icon], then [title] over [supporting]. Tappable when [onClick] is set. [iconTint] marks
 * it, such as `colorScheme.error` for a destructive action.
 */
@Composable
fun OrbitListCard(
    title: String,
    supporting: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleDecoration: TextDecoration = TextDecoration.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (trailing == null) {
        OrbitCard(modifier, onClick = onClick) {
            OrbitListItem(
                title = title,
                supporting = supporting,
                leading = { Icon(painterResource(icon), contentDescription = null, tint = iconTint) },
                titleDecoration = titleDecoration,
            )
        }
    } else {
        OrbitCard(modifier) {
            OrbitListItem(
                title = title,
                supporting = supporting,
                leading = { Icon(painterResource(icon), contentDescription = null, tint = iconTint) },
                trailing = trailing,
                titleDecoration = titleDecoration,
                onClick = onClick,
            )
        }
    }
}
