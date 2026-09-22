package com.orbit.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalOrbitColors = staticCompositionLocalOf { LightOrbitColors }

@Composable
fun OrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalOrbitColors provides if (darkTheme) DarkOrbitColors else LightOrbitColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = OrbitShapes,
            typography = OrbitTypography,
            content = content,
        )
    }
}

/** Orbit-specific theme values, alongside `MaterialTheme`. */
object OrbitTheme {
    val colors: OrbitColors
        @Composable @ReadOnlyComposable
        get() = LocalOrbitColors.current
}
