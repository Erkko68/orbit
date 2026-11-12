package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.InterviewQuestion

/** One message of the interview's chat: a question from the assistant, or what the user answered ([mine]). */
data class ChatMessage(val text: String, val mine: Boolean)

data class InterviewUiState(
    /** The user accepted what the assistant sends (#71): until then the consent notice is shown, and nothing is asked. */
    val consented: Boolean = false,
    /** The chat so far, oldest first. */
    val messages: List<ChatMessage> = emptyList(),
    /** The question to answer, the last of [messages]. Null while the assistant works, or after a failure. */
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
    /** The question takes an answer in the user's words. */
    val canType: Boolean get() = question?.freeText == true

    val canSend: Boolean
        get() = question != null && !loading && error == null && (selected.isNotEmpty() || text.isNotBlank())

    /** Asking again can help: a refusal (quota, switched off) only leaves closing the chat. */
    val canRetry: Boolean get() = error?.retryable == true
}
