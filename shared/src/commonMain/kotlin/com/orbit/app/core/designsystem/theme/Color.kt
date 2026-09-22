package com.orbit.app.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// The only place in the app where colors are defined. Contrast ratios are WCAG 2.x.

// Brand
val Violet = Color(0xFF6C63FF)
val Coral = Color(0xFFFF7A59)
val Teal = Color(0xFF2EC4B6)
val Sun = Color(0xFFFFC857)
val Navy = Color(0xFF14123B)
val NavyElevated = Color(0xFF221E5C)

// Neutrals
val White = Color(0xFFFFFFFF)
val LavenderSurface = Color(0xFFF3F2FF)
val Border = Color(0xFFDAD8F5)
val MutedText = Color(0xFF5B5A7A)
val LavenderText = Color(0xFFC9C6FF)
val LavenderMuted = Color(0xFFA9A6D6)
val PhoneShell = Color(0xFF0B0A24)

// Tonal variants derived from the brand hues (no new hues).
// Pure violet is 4.32:1 with white and 4.13:1 on navy, just under AA for body text.
private val VioletDeep = Color(0xFF6860F7)   // violet 4% toward navy: 4.56:1 with white
private val VioletBright = Color(0xFF857EFF) // violet 17% toward white: 5.45:1 on navy, 4.56:1 on navyElevated
private val VioletContainerLight = Color(0xFFE2E0FF) // violet 80% toward white
private val TealContainerLight = Color(0xFFD5F3F0)
private val CoralContainerLight = Color(0xFFFFE4DE)
private val VioletContainerDark = Color(0xFF332E80)  // violet 65% toward navy
private val TealContainerDark = Color(0xFF1D5066)
private val CoralContainerDark = Color(0xFF663646)

// Teal, coral and sun fail with white text (2.2, 2.6, 1.5:1), so their on-color is navy (6.9 to 11.6:1).
// Error keeps the M3 defaults: coral is for actions, not errors.
internal val LightColors = lightColorScheme(
    primary = VioletDeep,
    onPrimary = White,
    primaryContainer = VioletContainerLight,
    onPrimaryContainer = Navy,
    inversePrimary = VioletBright,
    secondary = Teal,
    onSecondary = Navy,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Navy,
    tertiary = Coral,
    onTertiary = Navy,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = Navy,
    background = White,
    onBackground = Navy,
    surface = White,
    onSurface = Navy,
    surfaceVariant = LavenderSurface,
    onSurfaceVariant = MutedText,
    surfaceContainerLowest = White,
    surfaceContainerLow = LavenderSurface,
    surfaceContainer = LavenderSurface,
    surfaceContainerHigh = LavenderSurface,
    surfaceContainerHighest = LavenderSurface,
    inverseSurface = Navy,
    inverseOnSurface = White,
    outline = Border,
    outlineVariant = Border,
)

internal val DarkColors = darkColorScheme(
    primary = VioletBright,
    onPrimary = Navy,
    primaryContainer = VioletContainerDark,
    onPrimaryContainer = White,
    inversePrimary = VioletDeep,
    secondary = Teal,
    onSecondary = Navy,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = White,
    tertiary = Coral,
    onTertiary = Navy,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = White,
    background = Navy,
    onBackground = White,
    surface = Navy,
    onSurface = White,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = LavenderText,
    surfaceContainerLowest = PhoneShell,
    surfaceContainerLow = Navy,
    surfaceContainer = NavyElevated,
    surfaceContainerHigh = NavyElevated,
    surfaceContainerHighest = NavyElevated,
    inverseSurface = White,
    inverseOnSurface = Navy,
    outline = Violet.copy(alpha = 0.5f),
    outlineVariant = NavyElevated,
)

/** Semantic colors outside the M3 scheme. Read them via `OrbitTheme.colors`. */
@Immutable
data class OrbitColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    /** Per-space identity fills (avatars, dots, borders). Decorative: never put body text on them. */
    val spaceAccents: List<Color>,
)

internal val LightOrbitColors = OrbitColors(
    success = Teal,
    onSuccess = Navy,
    warning = Sun,
    onWarning = Navy,
    info = VioletDeep,
    onInfo = White,
    spaceAccents = listOf(Violet, Coral, Teal, Sun),
)

internal val DarkOrbitColors = LightOrbitColors.copy(
    info = VioletBright,
    onInfo = Navy,
)
