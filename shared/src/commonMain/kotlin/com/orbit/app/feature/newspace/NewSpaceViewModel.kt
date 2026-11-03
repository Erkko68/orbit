package com.orbit.app.feature.newspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.attempt
import com.orbit.app.domain.model.JoinError
import com.orbit.app.domain.model.JoinException
import com.orbit.app.domain.model.invitationCodeOf
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Creates a space or joins one. An invitation link that brought the user here comes with its code ready to join. */
class NewSpaceViewModel(
    private val repository: SpaceRepository,
    pendingInvitation: PendingInvitation,
) : ViewModel() {
    private val form = MutableStateFlow(NewSpaceUiState(joinCode = pendingInvitation.code.value.orEmpty()))

    val uiState: StateFlow<NewSpaceUiState> = form

    init {
        pendingInvitation.consume()
    }

    fun onNameChange(name: String) = form.update { it.copy(name = name, createFailed = false) }

    fun selectAccent(accent: Int) = form.update { it.copy(accent = accent) }

    /** Creates a space owned by the signed-in user. */
    fun createSpace() {
        val current = form.value
        if (!current.canCreate) return
        form.update { it.copy(creating = true, createFailed = false) }
        viewModelScope.launch {
            attempt("Creating a space") { repository.createSpace(current.name.trim(), current.accent) }
                .onSuccess { id -> form.update { it.copy(creating = false, openSpaceId = id) } }
                .onFailure { form.update { it.copy(creating = false, createFailed = true) } }
        }
    }

    fun onJoinCodeChange(text: String) = form.update { it.copy(joinCode = text, joinError = null) }

    /** Joins the space the typed code or link invites to. */
    fun joinSpace() {
        val current = form.value
        if (!current.canJoin) return
        val code = invitationCodeOf(current.joinCode)
        if (code == null) {
            form.update { it.copy(joinError = JoinError.INVALID) }
            return
        }
        form.update { it.copy(joining = true, joinError = null) }
        viewModelScope.launch {
            attempt("Joining a space") { repository.joinSpace(code) }
                .onSuccess { id -> form.update { it.copy(joining = false, openSpaceId = id) } }
                .onFailure { cause ->
                    val error = (cause as? JoinException)?.error ?: JoinError.FAILED
                    form.update { it.copy(joining = false, joinError = error) }
                }
        }
    }
}
