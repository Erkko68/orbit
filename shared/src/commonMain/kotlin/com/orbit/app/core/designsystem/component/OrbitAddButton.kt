package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val AddSize = 62.dp
private val AddIconSize = 28.dp

/**
 * The one way to add something: a circle in the space's colour, floating over the content in the same place on every
 * screen. Pass it to [OrbitScreen]. [contentDescription] says what it adds.
 */
@Composable
fun OrbitAddButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AddCircle(Res.drawable.ic_add, contentDescription, onClick, modifier)
}

/** The add button's circle with any [icon], such as the close of [OrbitAddMenu]. */
@Composable
internal fun AddCircle(icon: DrawableResource, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(AddSize),
        shape = CircleShape,
        containerColor = OrbitTheme.accent.fill,
        contentColor = Ink,
    ) {
        Icon(painterResource(icon), contentDescription, Modifier.size(AddIconSize))
    }
}
