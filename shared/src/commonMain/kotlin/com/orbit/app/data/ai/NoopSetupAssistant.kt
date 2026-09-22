package com.orbit.app.data.ai

import com.orbit.app.domain.model.SetupAction

/** Stand-in until the platform LLM engines exist. */
class NoopSetupAssistant : SetupAssistant {
    override suspend fun isAvailable() = false
    override suspend fun plan(userText: String) = emptyList<SetupAction>()
}
