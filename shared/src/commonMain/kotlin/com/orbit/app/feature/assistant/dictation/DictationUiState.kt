package com.orbit.app.feature.assistant.dictation

import com.orbit.app.core.speech.DictationFailure

/** The microphone button of a text answer. */
data class DictationUiState(
    /** It is listening: the button stops it. */
    val listening: Boolean = false,
    /** The microphone has not been allowed yet and the system prompt is due. */
    val askingPermission: Boolean = false,
    /** Counts the dictations started, so the field knows where each one begins. */
    val started: Int = 0,
    /** What the current dictation has heard so far. */
    val transcript: String = "",
    /** Why the last dictation gave no text, said in a dialog until dismissed. Typing still works. */
    val failure: DictationFailure? = null,
)
