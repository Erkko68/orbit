package com.orbit.app.feature.auth

import com.orbit.app.core.attempt
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException

/** Runs an authentication [call] through [attempt] and returns why it failed, or null when it worked. */
internal suspend fun executeAuth(what: String, call: suspend () -> Unit): AuthError? =
    attempt(what, call).exceptionOrNull()?.let { (it as? AuthException)?.error ?: AuthError.UNKNOWN }
