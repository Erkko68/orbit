package com.orbit.app.data.repository

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** Stand-in until the Firebase repository replaces it (#15). Accepts any credentials. */
class InMemoryAuthRepository : AuthRepository {
    private val user = MutableStateFlow<User?>(null)

    override val session: Flow<User?> = user

    override suspend fun signIn(email: String, password: String) =
        register(email, password, displayName = email.substringBefore('@'))

    override suspend fun register(email: String, password: String, displayName: String) {
        user.value = User(
            id = email,
            displayName = displayName,
            photoUrl = null,
            language = "en",
            timeZone = TimeZone.currentSystemDefault(),
        )
    }

    override suspend fun signOut() {
        user.value = null
    }
}

internal suspend fun AuthRepository.requireUser(): User = checkNotNull(session.first()) { "Not signed in" }
