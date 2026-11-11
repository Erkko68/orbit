package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.AccountLinkException
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.LinkProof
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Accounts in memory. Signing in accepts any credentials; the password is only checked when changing it.
 * With a [collision] the way in it names is refused until the account is linked, and [linkFailure] fails
 * the linking itself (#173).
 */
class InMemoryAuthRepository(
    private val collision: AccountLinkException? = null,
    private val linkFailure: Exception? = null,
) : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    private var password = ""

    /** Whether the collision is still there: linking the account clears it. */
    private var collides = collision != null

    /** What was linked to the one account, in the order it happened. */
    val linked = mutableListOf<LinkProof>()

    override val session: Flow<User?> = user

    override suspend fun signIn(email: String, password: String) {
        collision?.takeIf { collides && it.proof == LinkProof.GOOGLE }?.let { throw it }
        register(email, password, displayName = email.substringBefore('@'))
    }

    override suspend fun register(email: String, password: String, displayName: String) {
        this.password = password
        user.value = User(
            id = email,
            displayName = displayName,
            photoUrl = null,
            email = email,
            hasPassword = password.isNotEmpty(),
        )
    }

    override suspend fun signInWithGoogle() {
        collision?.takeIf { collides && it.proof == LinkProof.PASSWORD }?.let { throw it }
        register("google@orbit.app", password = "", displayName = "Google User")
    }

    /** The password proves the account is theirs, so Google joins it: the same id, name and spaces. */
    override suspend fun linkGoogleWithPassword(email: String, password: String) {
        linkFailure?.let { throw it }
        collides = false
        linked += LinkProof.GOOGLE
        register(email, password, displayName = email.substringBefore('@'))
    }

    override suspend fun setPasswordWithGoogle(email: String, password: String) {
        linkFailure?.let { throw it }
        collides = false
        linked += LinkProof.PASSWORD
        register(email, password, displayName = email.substringBefore('@'))
    }

    /** The addresses a reset link was asked for, so a test can see one went out (#217). */
    val passwordResets = mutableListOf<String>()

    /** Never fails for an unknown address: the real one stays silent about those too. */
    override suspend fun sendPasswordReset(email: String) {
        passwordResets += email.trim()
    }

    override suspend fun signOut() {
        user.value = null
    }

    override suspend fun updateDisplayName(displayName: String) {
        val current = requireUser()
        require(displayName.isNotBlank()) { "A display name is required" }
        user.value = current.copy(displayName = displayName.trim())
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        requireUser()
        if (currentPassword != password) throw AuthException(AuthError.WRONG_CREDENTIALS)
        password = newPassword
    }

    override suspend fun idToken(): String = "token-${requireUser().id}"

    override suspend fun acceptAiConsent() {
        user.value = requireUser().copy(aiConsent = true)
    }

    override suspend fun reauthenticate(currentPassword: String?) {
        if (requireUser().hasPassword && currentPassword != password) throw AuthException(AuthError.WRONG_CREDENTIALS)
    }

    override suspend fun deleteAccount() {
        requireUser()
        user.value = null
    }
}
