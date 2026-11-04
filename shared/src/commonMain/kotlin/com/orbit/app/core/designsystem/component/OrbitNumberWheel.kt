package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * How many of something, from 0 to 99, on one wheel of [OrbitTimeWheel], beside the words that say of what. [description]
 * names it to a screen reader.
 */
@Composable
fun OrbitNumberWheel(value: Int, onValueChange: (Int) -> Unit, description: String, modifier: Modifier = Modifier) {
    Box(modifier) { Wheel(value, description, onValueChange, showLabel = false) }
}
