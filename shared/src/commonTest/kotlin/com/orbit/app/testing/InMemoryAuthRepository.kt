package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Accounts in memory. Signing in accepts any credentials; the password is only checked when changing it. */
class InMemoryAuthRepository : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    private var password = ""

    override val session: Flow<User?> = user

    override suspend fun signIn(email: String, password: String) =
        register(email, password, displayName = email.substringBefore('@'))

    override suspend fun register(email: String, password: String, displayName: String) {
        this.password = password
        user.value = User(
            id = email,
            displayName = displayName,
            photoUrl = null,
            email = email,
        )
    }

    override suspend fun signInWithGoogle() = register("google@orbit.app", password = "", displayName = "Google User")

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

    override suspend fun deleteAccount() {
        requireUser()
        user.value = null
    }
}
