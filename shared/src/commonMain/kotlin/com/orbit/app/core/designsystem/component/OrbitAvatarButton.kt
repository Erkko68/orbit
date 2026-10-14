package com.orbit.app.core.designsystem.component

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** An account avatar with an accessible action and Material's minimum touch target. */
@Composable
fun OrbitAvatarButton(
    initials: String,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        OrbitAvatar(initials, accent)
    }
}
