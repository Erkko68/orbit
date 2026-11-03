package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

// ponytail: a share of the window, not exactly what is left under the rows above it. A list measured to the pixel needs
// the header's height, which this cannot see.
private const val WINDOW_SHARE = 0.34f

/** The long text of something, typed where it is read: a panel that takes the room left on the screen, and grows with the text. */
@Composable
fun OrbitDetailsField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val height = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * WINDOW_SHARE).toDp() }
    OrbitCard(modifier) {
        OrbitNoteField(value, onValueChange, placeholder, onDone, Modifier.fillMaxWidth().heightIn(min = height), enabled)
    }
}
