package com.orbit.app.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// The only place in the app where colors are defined. Contrast ratios are WCAG 2.x, and ComponentContrastTest checks them.

// Ink and paper: text and the primary action, whatever the space's colour is.
val Ink = Color(0xFF0E1230)
val Paper = Color(0xFFF3F4F9)
val White = Color(0xFFFFFFFF)
/** The second line on a vivid fill: ink, a little softer. */
val InkSoft = Color(0xFF2A2E4E)

// Accents, in the order a space stores them: a stored index keeps its hue.
private val Nebula = Color(0xFF9A92FF)
private val Mars = Color(0xFFF0795A)
private val Uranus = Color(0xFF38BFB2)
private val Saturn = Color(0xFFE8B85A)
private val Neptune = Color(0xFF5B8DEF)
private val Venus = Color(0xFFE58BB0)

// Light neutrals. Text on them is at least 5.8:1, and 4.5:1 on any tinted page.
private val CanvasLight = Color(0xFFF3F4F8)
private val SunkLight = Color(0xFFEFF0F5)
private val HairLight = Color(0xFFE9EAF0)
private val InkSecondaryLight = Color(0xFF4A4F69)
private val InkTertiaryLight = Color(0xFF5F647E)

// Dark neutrals: panels step up from the canvas, and the sunk fill steps up again.
private val CanvasDark = Color(0xFF0A0C18)
private val PanelDark = Color(0xFF161A2C)
private val SunkDark = Color(0xFF21263C)
private val HairDark = Color(0xFF252A41)
private val InkSecondaryDark = Color(0xFFB4B9CE)
private val InkTertiaryDark = Color(0xFF989DB5)

// The assistant's violet.
private val AssistLight = Color(0xFF4F46D8)
private val AssistContainerLight = Color(0xFFE9E7FD)
private val OnAssistContainerLight = Color(0xFF2B2494)
private val AssistDark = Color(0xFFABA4FF)
private val AssistContainerDark = Color(0xFF211E52)
private val OnAssistContainerDark = Color(0xFFD9D6FF)

private val SuccessLight = Color(0xFF0A7268)
private val SuccessDark = Color(0xFF5FE0D2)
private val WarningLight = Color(0xFF8A5A00)
private val WarningDark = Color(0xFFFFD262)
private val DangerLight = Color(0xFFB3261E)
private val DangerDark = Color(0xFFFF8A80)

// Surface roles: `surface` is the page, `surfaceBright` a panel and `surfaceContainer` the sunk fill of a field or
// a chip. M3 components that default to the container roles read as panels or sunk fills without further styling.
// `primary` is ink, not a brand colour: the accent belongs to the space, and text never depends on it.
internal val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = White,
    primaryContainer = SunkLight,
    onPrimaryContainer = Ink,
    inversePrimary = Paper,
    secondary = AssistLight,
    onSecondary = White,
    secondaryContainer = AssistContainerLight,
    onSecondaryContainer = OnAssistContainerLight,
    tertiary = SuccessLight,
    onTertiary = White,
    tertiaryContainer = SunkLight,
    onTertiaryContainer = Ink,
    background = CanvasLight,
    onBackground = Ink,
    surface = CanvasLight,
    onSurface = Ink,
    surfaceVariant = SunkLight,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = SunkLight,
    surfaceContainerHigh = White,
    surfaceContainerHighest = SunkLight,
    surfaceBright = White,
    inverseSurface = Ink,
    inverseOnSurface = White,
    outline = InkTertiaryLight,
    outlineVariant = HairLight,
    error = DangerLight,
    onError = White,
)

internal val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = SunkDark,
    onPrimaryContainer = Paper,
    inversePrimary = Ink,
    secondary = AssistDark,
    onSecondary = Ink,
    secondaryContainer = AssistContainerDark,
    onSecondaryContainer = OnAssistContainerDark,
    tertiary = SuccessDark,
    onTertiary = Ink,
    tertiaryContainer = SunkDark,
    onTertiaryContainer = Paper,
    background = CanvasDark,
    onBackground = Paper,
    surface = CanvasDark,
    onSurface = Paper,
    surfaceVariant = SunkDark,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerLowest = PanelDark,
    surfaceContainerLow = PanelDark,
    surfaceContainer = SunkDark,
    surfaceContainerHigh = PanelDark,
    surfaceContainerHighest = SunkDark,
    surfaceBright = PanelDark,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    outline = InkTertiaryDark,
    outlineVariant = HairDark,
    error = DangerDark,
    onError = Ink,
)

/** The stored accent of everything outside a space: blue, because violet belongs to the assistant. */
internal const val BrandAccent = 4

/**
 * What a space's colour paints. [fill], [vivid] and [partner] always carry [Ink] text; [page] and [planet] carry the
 * theme's own ink; [selected] and [done] are fills on a panel. Read the current one with `OrbitTheme.accent`.
 */
@Immutable
data class OrbitAccent(
    /** Avatars, the add button and the mark. Decorative on the page: under 3:1 there. */
    val fill: Color,
    /** The loud fills of lists and chores. */
    val vivid: Color,
    /** The fill that sits next to [vivid]. */
    val partner: Color,
    /** The page behind the panels. */
    val page: Color,
    /** The disc behind the header. */
    val planet: Color,
    /** The soft ring around [planet]. */
    val halo: Color,
    /** The selected tab of the bar. */
    val selected: Color,
    /** A row that is done. */
    val done: Color,
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
    /** Per-space identity fills (avatars, the colour picker). Text on them is always [Ink]. */
    val spaceAccents: List<Color>,
    /** What each entry of [spaceAccents] paints in this theme. */
    val accents: List<OrbitAccent>,
    /** Ink for what must be read but stay quiet: an unticked check, an unselected tab. */
    val inkTertiary: Color,
    /** The soft shadow of panels. */
    val shadow: Color,
    /** A balance that gets money back, as a whole row. Text on it is always [Ink]. */
    val credit: Color,
    /** A balance that owes money, as a whole row. Text on it is always [Ink]. */
    val debit: Color,
) {
    /** The accent for [index] (a space's `accent`, a member's position), wrapping around the list. */
    fun spaceAccent(index: Int): Color = spaceAccents[index.mod(spaceAccents.size)]

    /** What the accent at [index] paints, wrapping around the list. */
    fun accent(index: Int): OrbitAccent = accents[index.mod(accents.size)]
}

/** A plain sRGB mix, as CSS `color-mix(in srgb, …)`: the audited ratios are for these exact values. */
private fun Color.mixedWith(other: Color, amount: Float) = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
)

private val SpaceAccents = listOf(Nebula, Mars, Uranus, Saturn, Neptune, Venus)

// Ink is 8:1 or better on every vivid and partner fill.
private val VividFills = listOf(
    Color(0xFFB1A6FF),
    Color(0xFFFF9A78),
    Color(0xFF4BDFCB),
    Color(0xFFFFD262),
    Color(0xFF86B6FF),
    Color(0xFFFFA3C8),
)
private val PartnerFills = listOf(
    Color(0xFFC9F06B),
    Color(0xFFFFD86B),
    Color(0xFFFFB4D2),
    Color(0xFF9FBEFF),
    Color(0xFFD4F26E),
    Color(0xFFA9EFD6),
)

private fun accents(canvas: Color, panel: Color, pageAmount: Float) = SpaceAccents.indices.map { index ->
    val fill = SpaceAccents[index]
    OrbitAccent(
        fill = fill,
        vivid = VividFills[index],
        partner = PartnerFills[index],
        page = canvas.mixedWith(fill, pageAmount),
        planet = canvas.mixedWith(fill, PlanetAmount),
        halo = fill.copy(alpha = HaloAlpha),
        selected = panel.mixedWith(fill, SelectedAmount),
        done = panel.mixedWith(VividFills[index], DoneAmount),
    )
}

private const val PageAmountLight = 0.20f
private const val PageAmountDark = 0.16f
private const val PlanetAmount = 0.34f
private const val HaloAlpha = 0.12f
private const val SelectedAmount = 0.22f
private const val DoneAmount = 0.22f

internal val LightOrbitColors = OrbitColors(
    success = SuccessLight,
    onSuccess = White,
    warning = WarningLight,
    onWarning = White,
    info = AssistLight,
    onInfo = White,
    spaceAccents = SpaceAccents,
    accents = accents(CanvasLight, White, PageAmountLight),
    inkTertiary = InkTertiaryLight,
    shadow = Ink,
    credit = VividFills[2],
    debit = VividFills[1],
)

internal val DarkOrbitColors = OrbitColors(
    success = SuccessDark,
    onSuccess = Ink,
    warning = WarningDark,
    onWarning = Ink,
    info = AssistDark,
    onInfo = Ink,
    spaceAccents = SpaceAccents,
    accents = accents(CanvasDark, PanelDark, PageAmountDark),
    inkTertiary = InkTertiaryDark,
    shadow = Color.Black,
    credit = VividFills[2],
    debit = VividFills[1],
)
