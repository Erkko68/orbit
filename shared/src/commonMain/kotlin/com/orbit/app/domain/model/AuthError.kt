package com.orbit.app.domain.model

/** Why signing in or registering failed, in terms a screen can explain to the user. */
enum class AuthError {
    /** The email and password match no account. Not "no such email": that would tell who has an account. */
    WRONG_CREDENTIALS,
    INVALID_EMAIL,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    UNKNOWN,
}

/** What an [com.orbit.app.domain.repository.AuthRepository] throws when signing in or registering fails. */
class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)
