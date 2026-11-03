package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_orbit
import org.jetbrains.compose.resources.painterResource

/** How big the mark is: [Small] in a top bar, [Medium] on a row, [Large] where it opens a screen. */
enum class OrbitMarkSize(internal val size: Dp) {
    Small(26.dp),
    Medium(44.dp),
    Large(56.dp),
}

/** The Orbit logo in the current space's colour. Decorative: the text next to it says what it is. */
@Composable
fun OrbitMark(
    modifier: Modifier = Modifier,
    size: OrbitMarkSize = OrbitMarkSize.Small,
) {
    Icon(
        painter = painterResource(Res.drawable.ic_orbit),
        contentDescription = null,
        modifier = modifier.size(size.size),
        tint = OrbitTheme.accent.fill,
    )
}
