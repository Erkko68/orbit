package com.orbit.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalOrbitColors = staticCompositionLocalOf { LightOrbitColors }
private val LocalOrbitAccent = staticCompositionLocalOf { LightOrbitColors.brand }
private val LocalOrbitData = staticCompositionLocalOf<OrbitDataTypography> { error("No OrbitTheme") }

@Composable
fun OrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkOrbitColors else LightOrbitColors
    CompositionLocalProvider(
        LocalOrbitColors provides colors,
        LocalOrbitAccent provides colors.brand,
        LocalOrbitData provides orbitDataTypography(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = OrbitShapes,
            typography = orbitTypography(),
            content = content,
        )
    }
}

/** Orbit-specific theme values, alongside `MaterialTheme`. */
object OrbitTheme {
    val colors: OrbitColors
        @Composable @ReadOnlyComposable
        get() = LocalOrbitColors.current

    /** What the accent paints: the add button, colour headers, vivid fills. */
    val accent: OrbitAccent
        @Composable @ReadOnlyComposable
        get() = LocalOrbitAccent.current

    /** Martian Mono, for amounts, times and dates. */
    val data: OrbitDataTypography
        @Composable @ReadOnlyComposable
        get() = LocalOrbitData.current
}
