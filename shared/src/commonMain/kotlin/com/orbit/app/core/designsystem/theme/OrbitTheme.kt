package com.orbit.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalOrbitColors = staticCompositionLocalOf { LightOrbitColors }
private val LocalOrbitAccent = staticCompositionLocalOf { LightOrbitColors.accent(BrandAccent) }
private val LocalOrbitData = staticCompositionLocalOf<OrbitDataTypography> { error("No OrbitTheme") }

@Composable
fun OrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkOrbitColors else LightOrbitColors
    CompositionLocalProvider(
        LocalOrbitColors provides colors,
        LocalOrbitAccent provides colors.accent(BrandAccent),
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

/** Paints [content] in the colour of a space: [accent] is the index the space stores, null until it has loaded. */
@Composable
fun OrbitSpaceColor(accent: Int?, content: @Composable () -> Unit) {
    val colors = OrbitTheme.colors
    CompositionLocalProvider(LocalOrbitAccent provides colors.accent(accent ?: BrandAccent), content = content)
}

/** Orbit-specific theme values, alongside `MaterialTheme`. */
object OrbitTheme {
    val colors: OrbitColors
        @Composable @ReadOnlyComposable
        get() = LocalOrbitColors.current

    /** What the current space's colour paints. Outside a space, the brand's. */
    val accent: OrbitAccent
        @Composable @ReadOnlyComposable
        get() = LocalOrbitAccent.current

    /** Martian Mono, for amounts, times and dates. */
    val data: OrbitDataTypography
        @Composable @ReadOnlyComposable
        get() = LocalOrbitData.current
}
