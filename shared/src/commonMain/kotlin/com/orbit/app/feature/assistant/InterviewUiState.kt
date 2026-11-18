package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.InterviewQuestion
import kotlinx.datetime.LocalDate

/**
 * One message of the interview's chat: a question from the assistant, or what the user answered ([mine]). An answer
 * with no [text] was skipped; a [decided] one is what the assistant chose for the user.
 */
data class ChatMessage(val text: String, val mine: Boolean, val decided: Boolean = false) {
    val skipped: Boolean get() = mine && text.isEmpty()
}

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
    /** The day a spent quota gives the assistant back, on this phone's calendar, when the server said. */
    val quotaResetsOn: LocalDate? = null,
    /** The assistant has a plan: the screen moves on to the review board. */
    val planReady: Boolean = false,
    /** How many questions are answered: Back returns to the last one. */
    val answered: Int = 0,
) {
    /** The question waits for the user: they can answer it, skip it or let the assistant decide. */
    val canAct: Boolean get() = question != null && !loading && error == null

    val canDecide: Boolean get() = canAct && question?.suggestion != null

    /** Back while nothing is on its way, after a failure too. */
    val canGoBack: Boolean get() = answered > 0 && !loading

    /** The question takes an answer in the user's words. */
    val canType: Boolean get() = question?.freeText == true

    val canSend: Boolean
        get() = canAct && (selected.isNotEmpty() || text.isNotBlank())

    /** Asking again can help: a refusal (quota, switched off) only leaves closing the chat. */
    val canRetry: Boolean get() = error?.retryable == true
}
