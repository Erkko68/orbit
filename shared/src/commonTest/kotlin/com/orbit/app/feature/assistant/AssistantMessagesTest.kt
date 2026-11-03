package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import kotlin.test.Test
import kotlin.test.assertEquals

class AssistantMessagesTest {
    @Test
    fun everyErrorHasItsOwnMessage() {
        val messages = AssistantError.entries.map { it.message() }

        assertEquals(AssistantError.entries.size, messages.toSet().size)
    }
}
