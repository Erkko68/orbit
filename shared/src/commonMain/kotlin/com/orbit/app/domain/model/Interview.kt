package com.orbit.app.domain.model

import kotlin.time.Instant

/**
 * One question of the assistant's interview, as the server sends it (#64): the chips in [options], several of them
 * when [multiSelect], and an answer in the user's words when [freeText]. How many questions come is the assistant's
 * call, so a question says nothing about how far along the interview is.
 */
data class InterviewQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val multiSelect: Boolean,
    val freeText: Boolean,
    /** What the assistant would answer: one of [options], or a few words. "Decide for me" answers with it (#74). */
    val suggestion: String? = null,
)

/**
 * What the user answered to the question [questionId], whose text was [question]: the chips they picked and what they
 * wrote, either may be empty, or both when they skipped it. The server keeps no state, so the question goes back with its answer (#66).
 */
data class InterviewAnswer(
    val questionId: String,
    val question: String,
    val selected: Set<String>,
    val text: String,
    /** The user let the assistant answer with its suggestion (#74). Nothing selected nor written is a skip. */
    val decided: Boolean = false,
)

/** What the assistant does with the answers so far: ask the next question, or say it has a plan to propose. */
sealed interface InterviewStep {
    data class Ask(val question: InterviewQuestion) : InterviewStep

    /** The assistant proposes [plan], which the review board (#75) shows. */
    data class PlanReady(val plan: Plan) : InterviewStep
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

/**
 * What an [com.orbit.app.domain.repository.AssistantRepository] throws when the assistant does not answer. A spent
 * quota says when the user has the assistant again ([resetsAt]).
 */
class AssistantException(
    val error: AssistantError,
    cause: Throwable? = null,
    val resetsAt: Instant? = null,
) : Exception(error.name, cause)
