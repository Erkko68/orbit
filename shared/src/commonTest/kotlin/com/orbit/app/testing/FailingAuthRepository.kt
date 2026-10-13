package com.orbit.app.testing

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Signed out, and signing in or registering fails with [failure] (by default as without a connection). */
internal class FailingAuthRepository(
    private val failure: Exception = IllegalStateException("No connection"),
) : AuthRepository {
    override val session: Flow<User?> = flowOf(null)

    override suspend fun signIn(email: String, password: String) = throw failure

    override suspend fun register(email: String, password: String, displayName: String) = throw failure

    override suspend fun signOut() = error("No connection")
}
