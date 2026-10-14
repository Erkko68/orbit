package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException

/** What went wrong signing in, from what Firebase threw. */
internal fun signInException(cause: Throwable) = AuthException(authErrorOf(cause, AuthError.WRONG_CREDENTIALS), cause)

/** What went wrong registering. There, Firebase rejects an email it does not accept as invalid credentials. */
internal fun registerException(cause: Throwable) = AuthException(authErrorOf(cause, AuthError.INVALID_EMAIL), cause)

/** What went wrong signing in with Google, from what Firebase threw. */
internal fun googleSignInException(cause: Throwable): Exception =
    (cause as? AuthException) ?: AuthException(authErrorOf(cause, AuthError.UNKNOWN), cause)

private fun authErrorOf(cause: Throwable, invalidCredentials: AuthError): AuthError = when (cause) {
    // Before invalid credentials: a weak password is one of them.
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> invalidCredentials
    is FirebaseNetworkException -> AuthError.NETWORK
    else -> AuthError.UNKNOWN
}
