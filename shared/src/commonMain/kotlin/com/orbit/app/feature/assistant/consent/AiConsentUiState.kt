package com.orbit.app.feature.assistant.consent

/** Asking the user to accept what the assistant sends to the AI provider, before its first use. */
data class AiConsentUiState(
    /** They have not accepted yet: the notice is open. */
    val asking: Boolean = false,
    /** Their acceptance is being saved: the accept button waits for it. */
    val submitting: Boolean = false,
    /** Saving their acceptance failed. Cleared by the next attempt. */
    val acceptFailed: Boolean = false,
    /** Whether they go on with the assistant, once that is settled: false means by hand. */
    val result: Boolean? = null,
)
