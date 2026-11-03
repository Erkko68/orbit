package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository

/** Calls `POST /interview` on the assistant server (ADR 0007). */
class ServerAssistantRepository : AssistantRepository {
    override suspend fun nextStep(answers: List<InterviewAnswer>): InterviewStep {
        Logger.w { "nextStep is a stub until the app calls the assistant server (#44)" }
        throw AssistantException(AssistantError.UNAVAILABLE)
    }
}
