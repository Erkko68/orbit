package com.orbit.app.testing

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Auth writes fail with [failure] (signing out as without a connection). Optionally starts signed in as [user]. */
internal class FailingAuthRepository(
    private val failure: Exception = IllegalStateException("No connection"),
    user: User? = null,
) : AuthRepository {
    override val session: Flow<User?> = flowOf(user)

    override suspend fun signIn(email: String, password: String) = throw failure

    override suspend fun register(email: String, password: String, displayName: String) = throw failure

    override suspend fun updateDisplayName(displayName: String) = throw failure

    override suspend fun changePassword(currentPassword: String, newPassword: String) = throw failure

    override suspend fun deleteAccount() = throw failure

    override suspend fun signInWithGoogle() = throw failure

    override suspend fun signOut() = error("No connection")
}
