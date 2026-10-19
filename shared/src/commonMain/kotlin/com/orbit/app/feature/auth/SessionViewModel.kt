package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** Backs the auth gate and decides where the app starts. */
class SessionViewModel(
    auth: AuthRepository,
    private val spaces: SpaceRepository,
    private val lastSpace: LastSpaceRepository,
) : ViewModel() {
    /**
     * Null until it is known, so neither side of the gate shows too early. Signed in, it is the space the
     * user opened last if they still belong to it, otherwise the list. It is worked out once per sign-in:
     * the screens do not move when the spaces change later.
     */
    val start: StateFlow<SessionStart?> = auth.session
        .map { it != null }
        .distinctUntilChanged()
        .transformLatest { signedIn ->
            if (signedIn) {
                emit(null)
                emit(startOfSignedInUser())
            } else {
                emit(SessionStart.SignedOut)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private suspend fun startOfSignedInUser(): SessionStart {
        val spaceId = lastSpace.lastSpaceId() ?: return SessionStart.Spaces
        val belongs = try {
            withTimeoutOrNull(SPACES_WAIT) { spaces.observeSpaces().first().any { it.id == spaceId } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Could not tell whether the last space still belongs to the user" }
            null
        }
        return when (belongs) {
            true -> SessionStart.Space(spaceId)
            false -> {
                lastSpace.forget()
                SessionStart.Spaces
            }
            // The spaces did not arrive in time: the list is a safe start, and the space stays remembered.
            null -> SessionStart.Spaces
        }
    }

    private companion object {
        val SPACES_WAIT = 2.seconds
    }
}
