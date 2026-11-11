package com.orbit.app.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// The only place in the app where colors are defined. Contrast ratios are WCAG 2.x, and ComponentContrastTest checks them.
//
// Three key colours, each used as tones of one palette (#293): a cool neutral for the page, panels and text, the brand
// blue and a warm coral. The number after a name is its tone, CIELAB L*: two tones 50 apart are at least 4.5:1, so the
// tone alone says whether text can sit on a fill. Error is the system's own colour, outside the three.

// Ink and paper: text and the primary action, whatever the space.
val Ink = Color(0xFF0E1230)
val Paper = Color(0xFFF3F4F9)
val White = Color(0xFFFFFFFF)
/** The second line on a vivid fill: ink, a little softer. */
val InkSoft = Color(0xFF2A2E4E)

// The cool neutral. Depth is its tones: the page, a panel on it, a sunk fill in the panel.
private val Neutral6 = Color(0xFF131318)
private val Neutral12 = Color(0xFF1F1F24)
private val Neutral17 = Color(0xFF2A292F)
private val Neutral20 = Color(0xFF303036)
private val Neutral30 = Color(0xFF47464C)
private val Neutral40 = Color(0xFF5E5E64)
private val Neutral50 = Color(0xFF77767D)
private val Neutral60 = Color(0xFF919097)
private val Neutral70 = Color(0xFFABAAB2)
private val Neutral80 = Color(0xFFC6C6CD)
private val Neutral90 = Color(0xFFE2E2E9)
private val Neutral94 = Color(0xFFEEEDF5)
private val Neutral95 = Color(0xFFF1F0F8)

// The brand blue.
private val Blue10 = Color(0xFF011B3D)
private val Blue17 = Color(0xFF022957)
private val Blue25 = Color(0xFF023A77)
private val Blue40 = Color(0xFF045DB7)
private val Blue60 = Color(0xFF5D8EF1)
private val Blue70 = Color(0xFF88A9FF)
private val Blue80 = Color(0xFFB5C5FE)
private val Blue90 = Color(0xFFDBE1FF)
private val Blue95 = Color(0xFFEDF0FE)

// The warm coral.
private val Coral40 = Color(0xFFA63A22)
private val Coral70 = Color(0xFFFF8B6C)
private val Coral80 = Color(0xFFFEB5A0)
private val Coral90 = Color(0xFFFFDBD1)

private val DangerLight = Color(0xFFB3261E)
private val DangerDark = Color(0xFFFF8A80)

// Surface roles: `surface` is the flat page, `surfaceBright` a panel and `surfaceContainer` the sunk fill of a field or
// a chip. M3 components that default to the container roles read as panels or sunk fills without further styling.
// `primary` is ink, not a brand colour: what matters most on a screen is the strongest contrast, not a hue.
internal val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = White,
    primaryContainer = Neutral95,
    onPrimaryContainer = Ink,
    inversePrimary = Paper,
    secondary = Blue40,
    onSecondary = White,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue10,
    tertiary = Coral40,
    onTertiary = White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Ink,
    background = Neutral94,
    onBackground = Ink,
    surface = Neutral94,
    onSurface = Ink,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral30,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = Neutral95,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Neutral95,
    surfaceBright = White,
    inverseSurface = Ink,
    inverseOnSurface = White,
    outline = Neutral50,
    outlineVariant = Neutral90,
    error = DangerLight,
    onError = White,
)

internal val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Neutral17,
    onPrimaryContainer = Paper,
    inversePrimary = Ink,
    secondary = Blue80,
    onSecondary = Ink,
    secondaryContainer = Blue25,
    onSecondaryContainer = Blue90,
    tertiary = Coral80,
    onTertiary = Ink,
    tertiaryContainer = Neutral17,
    onTertiaryContainer = Paper,
    background = Neutral6,
    onBackground = Paper,
    surface = Neutral6,
    onSurface = Paper,
    surfaceVariant = Neutral17,
    onSurfaceVariant = Neutral80,
    surfaceContainerLowest = Neutral12,
    surfaceContainerLow = Neutral12,
    surfaceContainer = Neutral17,
    surfaceContainerHigh = Neutral12,
    surfaceContainerHighest = Neutral17,
    surfaceBright = Neutral12,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    outline = Neutral60,
    outlineVariant = Neutral20,
    error = DangerDark,
    onError = Ink,
)

/** The stored accent of everything outside a space. Every index paints the same accent now (#293). */
internal const val BrandAccent = 0

/**
 * What the accent paints. [fill], [vivid] and [partner] always carry [Ink] text; [page] carries the theme's own ink;
 * [selected] and [done] are fills on a panel. Read the current one with `OrbitTheme.accent`.
 */
@Immutable
data class OrbitAccent(
    /** Avatars, the add button and the mark. Decorative on the page. */
    val fill: Color,
    /** The loud fills: a colour header, a selected day, the turn card's action. */
    val vivid: Color,
    /** The warm fill that sits next to [vivid]. */
    val partner: Color,
    /** The page behind the panels: one flat colour. */
    val page: Color,
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
    /** Fills that tell people apart (avatars) and the badges of spaces. Text on them is always [Ink]. */
    val spaceAccents: List<Color>,
    /** What the accent paints in this theme. */
    val brand: OrbitAccent,
    /** Ink for what must be read but stay quiet: an unticked check, an unselected tab. */
    val inkTertiary: Color,
    /** The soft shadow of panels. */
    val shadow: Color,
    /** A balance that gets money back, as a whole row. Text on it is always [Ink]. */
    val credit: Color,
    /** A balance that owes money, as a whole row. Text on it is always [Ink]. */
    val debit: Color,
) {
    /** The fill for [index] (a member's position, a space's badge), wrapping around the list. */
    fun spaceAccent(index: Int): Color = spaceAccents[index.mod(spaceAccents.size)]

    /** What the accent paints. The same for every space: a space is told apart by its badge, not its colour (#293). */
    @Suppress("UNUSED_PARAMETER")
    fun accent(index: Int): OrbitAccent = brand
}

// Tones of the blue and the coral, alternating so two faces side by side differ. Ink is 8:1 or better on each.
private val SpaceAccents = listOf(Blue80, Coral80, Blue70, Coral70, Blue90, Coral90)

internal val LightOrbitColors = OrbitColors(
    success = Blue40,
    onSuccess = White,
    warning = Coral40,
    onWarning = White,
    info = Blue40,
    onInfo = White,
    spaceAccents = SpaceAccents,
    brand = OrbitAccent(
        fill = Blue60,
        vivid = Blue80,
        partner = Coral80,
        page = Neutral94,
        selected = Blue90,
        done = Blue95,
    ),
    inkTertiary = Neutral40,
    shadow = Ink,
    credit = Blue80,
    debit = Coral80,
)

internal val DarkOrbitColors = OrbitColors(
    success = Blue80,
    onSuccess = Ink,
    warning = Coral80,
    onWarning = Ink,
    info = Blue80,
    onInfo = Ink,
    spaceAccents = SpaceAccents,
    brand = OrbitAccent(
        fill = Blue70,
        vivid = Blue80,
        partner = Coral80,
        page = Neutral6,
        selected = Blue25,
        done = Blue17,
    ),
    inkTertiary = Neutral70,
    shadow = Color.Black,
    credit = Blue80,
    debit = Coral80,
)
