package com.orbit.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One way to add, in an [OrbitAddMenu]. */
data class OrbitAddMenuAction(val label: String, val icon: DrawableResource, val onClick: () -> Unit)

/**
 * The add button when there is more than one way to add: tapped, it turns into a close button with [actions] fanned out
 * above it, the first one lowest. Picking one, the close button, a tap outside or back closes it. Whether it is
 * [expanded] belongs to the caller; [onExpandedChange] asks to change it.
 */
@Composable
fun OrbitAddMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    contentDescription: String,
    closeDescription: String,
    actions: List<OrbitAddMenuAction>,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        OrbitAddButton(contentDescription, { onExpandedChange(true) })
        if (!expanded) return@Box
        // Over the button, so the close sits where the add was, and focusable so a tap outside closes it without
        // reaching what is under it.
        Popup(
            alignment = Alignment.BottomEnd,
            onDismissRequest = { onExpandedChange(false) },
            properties = PopupProperties(focusable = true),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
                val shown = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = shown,
                    enter = fadeIn(OrbitMotion.effects()) +
                        scaleIn(OrbitMotion.spatial(), transformOrigin = TransformOrigin(1f, 1f)),
                ) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                        for (action in actions.asReversed()) {
                            // The content overload: the one with a text slot hides the text from screen readers.
                            ExtendedFloatingActionButton(
                                onClick = {
                                    onExpandedChange(false)
                                    action.onClick()
                                },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Icon(painterResource(action.icon), contentDescription = null)
                                Spacer(Modifier.width(OrbitSpacing.sm))
                                Text(action.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                AddCircle(Res.drawable.ic_close, closeDescription, { onExpandedChange(false) })
            }
        }
    }
}
