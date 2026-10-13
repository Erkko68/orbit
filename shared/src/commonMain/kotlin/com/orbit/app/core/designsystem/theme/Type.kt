package com.orbit.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Baseline = Typography()

// ponytail: M3 type scale in the platform font until a brand font is chosen. Headlines, titles and
// labels take the heavier weights of M3 Expressive's emphasized styles so the hierarchy reads at a glance.
internal val OrbitTypography = Baseline.copy(
    headlineLarge = Baseline.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Baseline.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Baseline.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Baseline.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Baseline.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Baseline.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Baseline.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Baseline.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)
