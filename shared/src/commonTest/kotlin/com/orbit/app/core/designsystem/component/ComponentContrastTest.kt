package com.orbit.app.core.designsystem.component

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.orbit.app.core.designsystem.theme.DarkColors
import com.orbit.app.core.designsystem.theme.LightColors
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every text color the base components draw passes WCAG AA (4.5:1) on each fill they draw it on. */
class ComponentContrastTest {

    private fun ColorScheme.textOnFill(): Map<String, Pair<Color, Color>> = mapOf(
        "button label" to (onPrimary to primary),
        "outlined button label, text on an elevated card or a dialog" to (onSurface to surfaceBright),
        "secondary text on an elevated card or a dialog" to (onSurfaceVariant to surfaceBright),
        "dialog actions" to (primary to surfaceBright),
        "text on a card or a field on the canvas" to (onSurface to surfaceContainer),
        "placeholder, icons and supporting text on a card or a field on the canvas" to
            (onSurfaceVariant to surfaceContainer),
        "top bar title, text on the canvas or a field on a card" to (onSurface to surface),
        "top bar subtitle, placeholder, icons and supporting text on the canvas or a field on a card" to
            (onSurfaceVariant to surface),
        "error on a card" to (error to surfaceContainer),
        "error on the canvas" to (error to surface),
        "avatar initials, the arrow of a card and an accent pill" to (onPrimaryContainer to primaryContainer),
        "text button on the canvas" to (primary to surface),
        "text button on a tonal card" to (primary to surfaceContainer),
        "neutral pill" to (onSurfaceVariant to surfaceContainerHigh),
        "success pill" to (onSecondaryContainer to secondaryContainer),
        "warning pill" to (onTertiaryContainer to tertiaryContainer),
        "text on an inset in a tonal card" to (onSurface to surfaceContainerLowest),
        "secondary text on an inset in a tonal card" to (onSurfaceVariant to surfaceContainerLowest),
        "text on an inset in an elevated card" to (onSurface to surfaceContainerLow),
        "secondary text on an inset in an elevated card" to (onSurfaceVariant to surfaceContainerLow),
        "extended FAB label" to (onTertiary to tertiary),
    )

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun assertAA(scheme: ColorScheme) = scheme.textOnFill().forEach { (use, colors) ->
        val ratio = contrast(colors.first, colors.second)
        assertTrue(ratio >= 4.5f, "$use is $ratio:1")
    }

    @Test
    fun lightSchemeIsAA() = assertAA(LightColors)

    @Test
    fun darkSchemeIsAA() = assertAA(DarkColors)
}
