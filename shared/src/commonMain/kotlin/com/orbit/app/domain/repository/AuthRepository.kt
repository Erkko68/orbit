package com.orbit.app.domain.repository

import com.orbit.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Sign-in, registration and sign-out need a connection and throw on failure. */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val session: Flow<User?>

    suspend fun signIn(email: String, password: String)

    suspend fun register(email: String, password: String, displayName: String)

    suspend fun signOut()
}
