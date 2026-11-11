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

    /**
     * Proves the signed-in user is the account's owner, as Firebase wants shortly before deleting it:
     * [currentPassword] for an account that has one, or null to ask Google again for one that only signs in
     * that way. Throws [com.orbit.app.domain.model.AuthError.WRONG_CREDENTIALS] when it does not check out.
     */
    suspend fun reauthenticate(currentPassword: String?)

    /**
     * Deletes the profile document and the account itself, which signs the user out. Called by
     * [com.orbit.app.domain.usecase.closeAccount], after [reauthenticate] and after the spaces (#188).
     */
    suspend fun deleteAccount()

    /**
     * Records that the signed-in user accepted what the assistant sends to the AI provider, so it is asked
     * once per account. Throws when signed out or when the write fails.
     */
    suspend fun acceptAiConsent()

    /** The signed-in user's Firebase ID token, which the assistant server checks (ADR 0007). Throws when signed out. */
    suspend fun idToken(): String

    /**
     * Emails [email] a link to set a new password (#217). An address with no account is not a failure and
     * nothing is sent for it: callers must answer the same either way, so nobody learns which addresses are
     * registered. Throws when the request itself fails, for example without a connection.
     */
    suspend fun sendPasswordReset(email: String)

    /** Shows the platform's Google account picker. Does nothing when the user closes it. */
    suspend fun signInWithGoogle()

    suspend fun signOut()
}
