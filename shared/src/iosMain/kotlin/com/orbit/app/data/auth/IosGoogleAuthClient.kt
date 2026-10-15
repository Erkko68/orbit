package com.orbit.app.data.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Google Sign-In SDK is a Swift package Kotlin cannot link, so the app implements this in Swift
 * (`GoogleSignInBridge.swift`). It calls [onResult] once: a token, `error`, or all null when the user
 * closed the picker.
 */
fun interface GoogleSignInBridge {
    fun signIn(onResult: (idToken: String?, accessToken: String?, error: String?) -> Unit)
}

class IosGoogleAuthClient(
    private val bridge: GoogleSignInBridge,
) : GoogleAuthClient {
    override suspend fun getCredential(): GoogleAuthCredential? = suspendCancellableCoroutine { cont ->
        bridge.signIn { idToken, accessToken, error ->
            when {
                error != null -> cont.resumeWithException(AuthException(AuthError.UNKNOWN, IllegalStateException(error)))
                idToken == null -> cont.resume(null)
                else -> cont.resume(GoogleAuthCredential(idToken, accessToken))
            }
        }
    }
}
