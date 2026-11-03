package com.orbit.app.core.designsystem.component

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.orbit.app.core.designsystem.theme.DarkColors
import com.orbit.app.core.designsystem.theme.LightColors
import com.orbit.app.core.designsystem.theme.OrbitColors
import com.orbit.app.core.designsystem.theme.DarkOrbitColors
import com.orbit.app.core.designsystem.theme.LightOrbitColors
import com.orbit.app.core.designsystem.theme.InkSoft
import com.orbit.app.core.designsystem.theme.Ink
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every text color the base components draw passes WCAG AA (4.5:1) on each fill they draw it on. */
class ComponentContrastTest {

    private fun ColorScheme.textOnFill(): Map<String, Pair<Color, Color>> = mapOf(
        "button label" to (onPrimary to primary),
        "text and text buttons on a panel, a sheet or a dialog" to (onSurface to surfaceBright),
        "secondary text on a panel, a sheet or a dialog" to (onSurfaceVariant to surfaceBright),
        "text on a field, a chip or a tonal button" to (onSurface to surfaceContainer),
        "placeholder, icons and the neutral pill on a sunk fill" to (onSurfaceVariant to surfaceContainer),
        "error on a panel" to (error to surfaceBright),
        "error on a field" to (error to surfaceContainer),
        "turn card and floating field" to (inverseOnSurface to inverseSurface),
        "on the assistant's soft violet" to (onSecondaryContainer to secondaryContainer),
        "the assistant's violet as text on a panel" to (secondary to surfaceBright),
    )

    /** Each accent, over what it paints. The page and the planet carry the theme's ink, the fills always carry navy. */
    private fun OrbitColors.textOnAccents(scheme: ColorScheme): Map<String, Pair<Color, Color>> =
        accents.flatMapIndexed { index, accent ->
            listOf(
                "text on the page of accent $index" to (scheme.onSurface to accent.page),
                "secondary text on the page of accent $index" to (scheme.onSurfaceVariant to accent.page),
                "the section count and header on the planet of accent $index" to (scheme.onSurfaceVariant to accent.planet),
                "the top bar on the planet of accent $index" to (scheme.onSurface to accent.planet),
                "the add button of accent $index" to (Ink to accent.fill),
                "a face in accent $index" to (Ink to spaceAccent(index)),
                "a tile and a header on vivid $index" to (Ink to accent.vivid),
                "the peek of a tile on vivid $index" to (InkSoft to accent.vivid),
                "a tile on partner $index" to (Ink to accent.partner),
                "the peek of a tile on partner $index" to (InkSoft to accent.partner),
                "the selected tab of accent $index" to (scheme.onSurface to accent.selected),
                "a row that is done in accent $index" to (scheme.onSurface to accent.done),
                "the quiet title of a row that is done in accent $index" to (scheme.onSurfaceVariant to accent.done),
                "the turn card's action on accent $index" to (Ink to accent.vivid),
            )
        }.toMap()

    private fun OrbitColors.textOnStates(scheme: ColorScheme): Map<String, Pair<Color, Color>> = mapOf(
        "success pill" to (onSuccess to success),
        "warning pill" to (onWarning to warning),
        "info" to (onInfo to info),
        "an unselected tab" to (inkTertiary to scheme.surfaceBright),
    )

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun assertAA(pairs: Map<String, Pair<Color, Color>>) = pairs.forEach { (use, colors) ->
        val ratio = contrast(colors.first, colors.second)
        assertTrue(ratio >= 4.5f, "$use is $ratio:1")
    }

    @Test
    fun lightSchemeIsAA() = assertAA(LightColors.textOnFill())

    @Test
    fun darkSchemeIsAA() = assertAA(DarkColors.textOnFill())

    @Test
    fun lightAccentsAreAA() = assertAA(LightOrbitColors.textOnAccents(LightColors))

    @Test
    fun darkAccentsAreAA() = assertAA(DarkOrbitColors.textOnAccents(DarkColors))

    @Test
    fun lightStatesAreAA() = assertAA(LightOrbitColors.textOnStates(LightColors))

    @Test
    fun darkStatesAreAA() = assertAA(DarkOrbitColors.textOnStates(DarkColors))
}
