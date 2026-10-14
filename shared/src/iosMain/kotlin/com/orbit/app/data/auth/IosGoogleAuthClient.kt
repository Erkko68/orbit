package com.orbit.app.data.auth

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException

class IosGoogleAuthClient : GoogleAuthClient {
    override suspend fun getCredential(): GoogleAuthCredential? {
        Logger.w { "Google Sign-In on iOS requires native client configuration (see #174)" }
        throw AuthException(
            AuthError.UNKNOWN,
            IllegalStateException("Google Sign-In on iOS requires bundle id and client configuration in Firebase"),
        )
    }
}
