package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.InterviewQuestion

data class InterviewUiState(
    /** The user accepted what the assistant sends (#71): until then the consent notice is shown, and nothing is asked. */
    val consented: Boolean = false,
    /** The question on screen. Null until the first one arrives. */
    val question: InterviewQuestion? = null,
    /** The chips picked for [question]. */
    val selected: Set<String> = emptySet(),
    /** What is written for [question], typed or dictated. */
    val text: String = "",
    /** The assistant is working on the next step. */
    val loading: Boolean = false,
    /** Why the interview stopped. Cleared by the next attempt. */
    val error: AssistantError? = null,
    /** The assistant has a plan: the screen moves on to the review board. */
    val planReady: Boolean = false,
) {
    val canSend: Boolean
        get() = question != null && !loading && error == null && (selected.isNotEmpty() || text.isNotBlank())

    /** Asking again can help: a refusal (quota, switched off) only leaves the manual path. */
    val canRetry: Boolean get() = error?.retryable == true
}
