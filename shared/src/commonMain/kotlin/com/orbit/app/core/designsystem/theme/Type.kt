package com.orbit.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.jost_bold
import orbit.shared.generated.resources.jost_medium
import orbit.shared.generated.resources.jost_regular
import orbit.shared.generated.resources.jost_semibold
import orbit.shared.generated.resources.martian_mono_medium
import orbit.shared.generated.resources.martian_mono_semibold
import org.jetbrains.compose.resources.Font

private const val TIGHT = -0.015f
private const val SNUG = -0.01f

private fun style(family: FontFamily, size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

/**
 * Jost for everything: big and bold. Jost reads small for its size, so body text starts at 15. Static TTFs,
 * because variable font axes on iOS are not confirmed. Licences are in `files/licenses`.
 */
@Composable
internal fun orbitTypography(): Typography {
    val regular = Font(Res.font.jost_regular, FontWeight.Normal)
    val medium = Font(Res.font.jost_medium, FontWeight.Medium)
    val semibold = Font(Res.font.jost_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.jost_bold, FontWeight.Bold)
    return remember(regular, medium, semibold, bold) {
        val jost = FontFamily(regular, medium, semibold, bold)
        Typography(
            displayLarge = style(jost, 46, 48, FontWeight.Bold, TIGHT),
            displayMedium = style(jost, 42, 44, FontWeight.Bold, TIGHT),
            displaySmall = style(jost, 36, 38, FontWeight.Bold, TIGHT),
            headlineLarge = style(jost, 32, 34, FontWeight.Bold, TIGHT),
            headlineMedium = style(jost, 28, 32, FontWeight.Bold, SNUG),
            headlineSmall = style(jost, 26, 30, FontWeight.Bold, SNUG),
            titleLarge = style(jost, 24, 28, FontWeight.Bold, SNUG),
            titleMedium = style(jost, 19, 23, FontWeight.SemiBold),
            titleSmall = style(jost, 18, 22, FontWeight.SemiBold),
            bodyLarge = style(jost, 18, 24, FontWeight.Normal),
            bodyMedium = style(jost, 16, 22, FontWeight.Normal),
            bodySmall = style(jost, 15, 19, FontWeight.Normal),
            labelLarge = style(jost, 16, 20, FontWeight.SemiBold),
            labelMedium = style(jost, 15, 18, FontWeight.SemiBold),
            labelSmall = style(jost, 12, 16, FontWeight.Medium),
        )
    }
}

/** Martian Mono, as the voice of amounts, times and dates. */
@Immutable
data class OrbitDataTypography(
    /** A number to pick, such as a time on a wheel. */
    val large: TextStyle,
    /** An amount in a row. */
    val medium: TextStyle,
    /** A date, a count: set in capitals by the caller when it is a label. */
    val small: TextStyle,
)

@Composable
internal fun orbitDataTypography(): OrbitDataTypography {
    val medium = Font(Res.font.martian_mono_medium, FontWeight.Medium)
    val semibold = Font(Res.font.martian_mono_semibold, FontWeight.SemiBold)
    return remember(medium, semibold) {
        val mono = FontFamily(medium, semibold)
        OrbitDataTypography(
            large = style(mono, 20, 26, FontWeight.SemiBold, -0.02f),
            medium = style(mono, 14, 18, FontWeight.SemiBold, -0.02f),
            small = style(mono, 11, 14, FontWeight.Medium, 0.02f),
        )
    }
}
