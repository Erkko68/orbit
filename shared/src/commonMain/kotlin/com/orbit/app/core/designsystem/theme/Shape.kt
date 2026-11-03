package com.orbit.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** A panel that rises from the bottom edge: the bar, a sheet, a form. */
internal val PanelTopShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

// Panels are large, controls medium, and what is loud (a list tile, the turn card) extra large.
internal val OrbitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
