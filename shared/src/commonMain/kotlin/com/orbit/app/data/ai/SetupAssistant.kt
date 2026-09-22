package com.orbit.app.data.ai

import com.orbit.app.domain.model.SetupAction

/** Turns a free-form group description into proposed actions using the on-device model. */
interface SetupAssistant {
    suspend fun isAvailable(): Boolean
    suspend fun plan(userText: String): List<SetupAction>
}
