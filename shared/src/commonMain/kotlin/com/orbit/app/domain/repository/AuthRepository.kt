package com.orbit.app.domain.repository

import com.orbit.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Sign-in, registration and sign-out need a connection and throw on failure. Signing in and
 * registering throw an [com.orbit.app.domain.model.AuthException] that says why.
 */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val session: Flow<User?>

    suspend fun signIn(email: String, password: String)

    suspend fun register(email: String, password: String, displayName: String)

    /** Renames the signed-in user. Throws when signed out or when the write fails. */
    suspend fun updateDisplayName(displayName: String)

    /**
     * Changes the signed-in user's password once [currentPassword] checks out. Throws an
     * [com.orbit.app.domain.model.AuthException]: wrong credentials when it does not, a weak password
     * when the new one is refused.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String)

    /** Deletes the signed-in user's account, which signs them out. */
    suspend fun deleteAccount()

    /** Shows the platform's Google account picker. Does nothing when the user closes it. */
    suspend fun signInWithGoogle()

    suspend fun signOut()
}
