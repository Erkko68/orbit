package com.orbit.app.core.speech

import kotlinx.coroutines.flow.Flow

/**
 * Speech to text through the platform's own recogniser, in the phone's language (ADR 0006). The microphone
 * permission is asked with [rememberDictationPermissionPrompt].
 */
interface Dictation {
    /** The phone has a recogniser for its language. */
    fun isAvailable(): Boolean

    /** The user lets the app use the microphone and, where the platform asks for it, speech recognition. */
    suspend fun hasPermission(): Boolean

    /**
     * Listens until the user stops talking, emitting the whole text heard so far each time it grows, then
     * completes. Cancel the collection to stop early: the last text emitted stays. Throws a
     * [DictationException] when it cannot listen or hears nothing.
     */
    fun listen(): Flow<String>
}

/** Why dictation stopped without text, in terms the button can explain. */
enum class DictationFailure {
    /** The microphone or speech recognition is not allowed. */
    NO_PERMISSION,

    /** No recogniser for the phone's language. */
    UNAVAILABLE,

    /** It listened and heard nothing it could write. */
    NO_SPEECH,

    /** Anything else: the recogniser failed or lost its connection. */
    ERROR,
}

class DictationException(val failure: DictationFailure) : Exception(failure.name)
