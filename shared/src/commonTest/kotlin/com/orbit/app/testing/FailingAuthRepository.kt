package com.orbit.app.testing

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Signed out, and every call fails as it would without a connection. */
internal class FailingAuthRepository : AuthRepository {
    override val session: Flow<User?> = flowOf(null)

    override suspend fun signIn(email: String, password: String) = error("No connection")

    override suspend fun register(email: String, password: String, displayName: String) = error("No connection")

    override suspend fun signOut() = error("No connection")
}
