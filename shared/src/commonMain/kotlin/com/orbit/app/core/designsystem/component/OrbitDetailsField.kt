package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The long text of something, typed where it is read: a panel as tall as its text, which grows as it is typed. */
@Composable
fun OrbitDetailsField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OrbitCard(modifier) {
        OrbitNoteField(value, onValueChange, placeholder, onDone, Modifier.fillMaxWidth(), enabled)
    }
}
