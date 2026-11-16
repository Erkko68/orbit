package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private val COMMIT_WAIT = 3.seconds
private val SERVER_WAIT = 10.seconds

/**
 * Runs a Firestore [write], which applies locally at once and completes when the server confirms it.
 * Offline, a write still unconfirmed after [COMMIT_WAIT] stays queued and Firestore keeps retrying it,
 * so this returns instead of hanging the caller. One the server refuses in time (the rules said no) throws.
 */
internal suspend fun writeLocally(what: String, write: suspend () -> Unit) {
    try {
        withTimeout(COMMIT_WAIT) { write() }
    } catch (_: TimeoutCancellationException) {
        Logger.i { "$what is saved on this device and waiting for the connection" }
    }
}

/**
 * Runs a Firestore [write] that is only any use once the server has it: an invitation nobody can use until it is
 * there, or a join. Unlike [writeLocally] it does not settle for the local copy: with no answer from the server within
 * ten seconds it throws, so the user hears it needs a connection instead of waiting on, or sharing, something that does
 * not work yet. The write stays queued, and Firestore sends it when the connection is back. A refusal throws too.
 */
internal suspend fun writeOnServer(what: String, write: suspend () -> Unit) {
    try {
        withTimeout(SERVER_WAIT) { write() }
    } catch (e: TimeoutCancellationException) {
        throw IllegalStateException("The server did not confirm $what in time", e)
    }
}
