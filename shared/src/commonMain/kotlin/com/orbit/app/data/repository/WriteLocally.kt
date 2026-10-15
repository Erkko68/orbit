package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

private val COMMIT_WAIT = 3.seconds

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
