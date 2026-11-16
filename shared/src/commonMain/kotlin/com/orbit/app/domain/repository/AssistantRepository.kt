package com.orbit.app.domain.repository

import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep

/** The assistant on our own server (ADR 0007). It only proposes: nothing it returns is saved here. */
fun interface AssistantRepository {
    /**
     * The next step of the interview for [spaceId], or for a new space when it is empty, given every [answers] so far,
     * none for the first question: the server keeps no state between calls (#64), so each call sends the space's
     * members and items too (#66). Throws an [com.orbit.app.domain.model.AssistantException] when it does not answer.
     */
    suspend fun nextStep(spaceId: String, answers: List<InterviewAnswer>): InterviewStep
}
