package com.orbit.app.domain.repository

import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep

/** The assistant on our own server (ADR 0007). It only proposes: nothing it returns is saved here. */
interface AssistantRepository {
    /**
     * The next step of the interview given every [answers] so far, none for the first question: the server keeps
     * no state between calls (#64). Throws an [com.orbit.app.domain.model.AssistantException] when it does not answer.
     */
    suspend fun nextStep(answers: List<InterviewAnswer>): InterviewStep
}
