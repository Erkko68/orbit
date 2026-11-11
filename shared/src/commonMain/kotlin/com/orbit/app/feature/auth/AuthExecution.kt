package com.orbit.app.feature.auth

import com.orbit.app.core.attempt
import com.orbit.app.domain.model.AccountLinkException
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.LinkProof

/** Runs an authentication [call] through [attempt] and returns why it failed, or null when it worked. */
internal suspend fun executeAuth(what: String, call: suspend () -> Unit): AuthError? =
    attempt(what, call).exceptionOrNull()?.let { (it as? AuthException)?.error ?: AuthError.UNKNOWN }

/** What a sign-in left behind: the account it belongs to, or why it failed. */
internal sealed interface SignInOutcome {
    /** The address belongs to one account reached another way, which the user can link (#173). */
    data class Link(val email: String, val proof: LinkProof) : SignInOutcome

    data class Failed(val error: AuthError) : SignInOutcome
}

/**
 * Runs a sign-in [call] through [attempt] and returns what it left behind, or null when the user is in.
 * A collision is a [SignInOutcome.Link] the form offers, not a failure it blames the user for.
 */
internal suspend fun executeSignIn(what: String, call: suspend () -> Unit): SignInOutcome? =
    attempt(what, call).exceptionOrNull()?.let { cause ->
        when (cause) {
            is AccountLinkException -> SignInOutcome.Link(cause.email, cause.proof)
            is AuthException -> SignInOutcome.Failed(cause.error)
            else -> SignInOutcome.Failed(AuthError.UNKNOWN)
        }
    }
