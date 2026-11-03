package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * An action shown as an [icon] only, such as one in a top bar. [contentDescription] names it for screen readers.
 * [raised] puts it on a soft panel, for one that sits on the page.
 */
@Composable
fun OrbitIconButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    raised: Boolean = false,
) {
    val shape = MaterialTheme.shapes.medium
    IconButton(
        onClick = onClick,
        modifier = if (raised) modifier.panelShadow(shape) else modifier,
        shape = shape,
        colors = if (raised) {
            IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            IconButtonDefaults.iconButtonColors()
        },
    ) {
        Icon(painterResource(icon), contentDescription)
    }
}
