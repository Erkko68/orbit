package com.orbit.app.domain.repository

import com.orbit.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/**
 * Sign-in, registration and sign-out need a connection and throw on failure. Signing in and
 * registering throw an [com.orbit.app.domain.model.AuthException] that says why.
 */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val session: Flow<User?>

    suspend fun signIn(email: String, password: String)

    suspend fun register(email: String, password: String, displayName: String)

    /** Updates the signed-in user's profile. Throws when signed out or when the write fails. */
    suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone)

    /** Shows the platform's Google account picker. Does nothing when the user closes it. */
    suspend fun signInWithGoogle()

    suspend fun signOut()
}
