package com.orbit.app.feature.auth

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.CancellationException

/**
 * Runs an authentication [block] and returns the resulting [AuthError], or null on success.
 * Catches failures, rethrows [CancellationException], and logs unexpected errors.
 */
internal suspend fun executeAuth(
    actionName: String,
    block: suspend () -> Unit,
): AuthError? = try {
    block()
    null
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Logger.e(e) { "$actionName failed" }
    (e as? AuthException)?.error ?: AuthError.UNKNOWN
}
