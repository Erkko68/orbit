package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** How long the view model gets to answer a key press before what it says counts as a change of its own. */
private const val ANSWER_FRAMES = 2

/**
 * The text and cursor of a field whose [value] comes back from a view model a frame after the key press. A `String`
 * field recomposes with the old text in between and puts the cursor back at the start, so "hello" comes out as "elloh".
 * This holds what is on screen and takes [value] only when it still differs once the typing has settled: the caller
 * cleared the field, or kept less than was typed.
 */
@Composable
internal fun rememberFieldValue(value: String): MutableState<TextFieldValue> {
    val field = remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value, field.value.text) {
        repeat(ANSWER_FRAMES) { withFrameNanos {} }
        if (value != field.value.text) field.value = TextFieldValue(value, TextRange(value.length))
    }
    return field
}
