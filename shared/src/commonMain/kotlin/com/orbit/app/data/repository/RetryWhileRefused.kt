package com.orbit.app.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Listens again, up to [retries] times and [wait] apart, while the listener fails with something
 * [isRefused] recognises. A space just made is on screen from the local write before the server has it, and
 * the rules only let a member of a space that exists read what is in it, so the first listen can be refused.
 * Any other failure, or the last refusal, goes on to the caller.
 */
internal fun <T> Flow<T>.retryWhileRefused(
    retries: Int = 5,
    wait: Duration = 1.seconds,
    isRefused: (Throwable) -> Boolean = Throwable::isRefusedByTheRules,
): Flow<T> = retryWhen { cause, attempt ->
    val again = attempt < retries && isRefused(cause)
    if (again) delay(wait)
    again
}
