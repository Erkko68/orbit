package com.orbit.app.domain.model

/**
 * One question of the assistant's interview, as the server sends it (#64): the chips in [options], several of them
 * when [multiSelect], and an answer in the user's words when [freeText]. [position] of [budget] says how far along
 * the interview is.
 */
data class InterviewQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val multiSelect: Boolean,
    val freeText: Boolean,
    val position: Int,
    val budget: Int,
)

/** What the user answered to the question [questionId]: the chips they picked and what they wrote, either may be empty. */
data class InterviewAnswer(val questionId: String, val selected: Set<String>, val text: String)

/** What the assistant does with the answers so far: ask the next question, or say it has a plan to propose. */
sealed interface InterviewStep {
    data class Ask(val question: InterviewQuestion) : InterviewStep

    /** The assistant has enough to propose a plan, which the review board (#75) shows. */
    data object PlanReady : InterviewStep
}

/** Why the assistant did not answer, in terms a screen can explain. */
enum class AssistantError {
    NETWORK,

    /** The user spent their share of the assistant for now (#67). */
    QUOTA_EXCEEDED,

    /** The assistant is switched off on the server (#68). */
    DISABLED,

    /** The server or its model is down, or answered something it could not use. */
    UNAVAILABLE,
    UNKNOWN,
    ;

    /** Asking again can work. A refusal does not change by asking again. */
    val retryable: Boolean get() = this != QUOTA_EXCEEDED && this != DISABLED
}

/** What an [com.orbit.app.domain.repository.AssistantRepository] throws when the assistant does not answer. */
class AssistantException(val error: AssistantError, cause: Throwable? = null) : Exception(error.name, cause)
