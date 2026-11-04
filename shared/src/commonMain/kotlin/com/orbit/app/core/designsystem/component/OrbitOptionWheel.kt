package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val OptionWheelWidth = 120.dp

/**
 * One of a few [options] on a wheel, beside an [OrbitNumberWheel] to say so many of what: "2 weeks". The wheel scrolls
 * and snaps to an option, and tapping one goes to it. [selected] is its index.
 */
@Composable
fun OrbitOptionWheel(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        Wheel(
            value = selected,
            label = "",
            onValueChange = onSelect,
            words = options,
            showLabel = false,
            width = OptionWheelWidth,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
