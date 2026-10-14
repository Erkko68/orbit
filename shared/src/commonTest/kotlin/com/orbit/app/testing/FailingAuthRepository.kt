package com.orbit.app.testing

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone

/** Auth writes fail with [failure]. Optionally starts with a signed-in user for profile tests. */
internal class FailingAuthRepository(
    private val failure: Exception = IllegalStateException("No connection"),
    user: User? = null,
) : AuthRepository {
    override val session: Flow<User?> = flowOf(user)

    override suspend fun signIn(email: String, password: String) = throw failure

    override suspend fun register(email: String, password: String, displayName: String) = throw failure

    override suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone) =
        error("No connection")

    override suspend fun signInWithGoogle(idToken: String?, accessToken: String?) = throw failure

    override suspend fun signOut() = error("No connection")
}
