package com.orbit.app.testing

import com.orbit.app.core.speech.Dictation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Dictation the test drives by hand: [say] what the user says, [finish] to stop talking or [fail] to make
 * the recogniser give up. Counts how often it listened.
 */
class FakeDictation(var available: Boolean = true, var allowed: Boolean = true) : Dictation {
    private var heard = Channel<String>(Channel.UNLIMITED)

    var listens = 0
        private set

    override fun isAvailable(): Boolean = available

    override suspend fun hasPermission(): Boolean = allowed

    override fun listen(): Flow<String> {
        listens++
        heard = Channel(Channel.UNLIMITED)
        return heard.receiveAsFlow()
    }

    fun say(text: String) {
        heard.trySend(text)
    }

    fun finish() {
        heard.close()
    }

    fun fail(cause: Exception) {
        heard.close(cause)
    }
}
