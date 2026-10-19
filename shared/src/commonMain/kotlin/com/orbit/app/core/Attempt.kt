package com.orbit.app.core

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException

/**
 * Runs a repository [write] and returns its result. A failure is logged as [what] and returned, never
 * thrown, so a failed write cannot crash the app. Cancellation still propagates.
 */
suspend fun <T> attempt(what: String, write: suspend () -> T): Result<T> = try {
    Result.success(write())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Logger.e(e) { "$what failed" }
    Result.failure(e)
}
