package com.orbit.app.feature.spacesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The settings of a space: an owner or admin changes its name and colour, the owner deletes it, anyone else leaves. */
class SpaceSettingsViewModel(
    private val spaceId: String,
    private val spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    /** What the user is doing on the screen. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(SpaceSettingsUiState())

    /** Members who cannot be read cannot be checked, so nothing can be edited until they load. */
    val uiState: StateFlow<SpaceSettingsUiState> = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
        form,
    ) { all, members, user, form ->
        val space = all.firstOrNull { it.id == spaceId }
        form.copy(
            role = members.firstOrNull { it.userId == user?.id }?.role,
            spaceName = space?.name.orEmpty(),
            accent = space?.accent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceSettingsUiState())

    fun onNameChange(name: String) = form.update { it.copy(nameDraft = name, failed = false) }

    fun onAccentChange(accent: Int) = form.update { it.copy(accentDraft = accent, failed = false) }

    /** Saves what changed, and only that. A failure keeps what was typed and says so. */
    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val name = state.name.trim().takeIf { it != state.spaceName }
        val accent = state.accentDraft?.takeIf { it != state.accent }
        form.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            attempt("Saving the settings of space $spaceId") { spaces.updateSpace(spaceId, name, accent) }
                .onSuccess { form.update { it.copy(nameDraft = null, accentDraft = null, saving = false) } }
                .onFailure { form.update { it.copy(saving = false, failed = true) } }
        }
    }

    /** Asks to confirm leaving the space, or deleting it for the owner. */
    fun confirmExit() {
        if (uiState.value.role != null) form.update { it.copy(confirmingExit = true, exitFailed = false) }
    }

    fun cancelExit() {
        if (!form.value.exiting) form.update { it.copy(confirmingExit = false, exitFailed = false) }
    }

    /** Deletes the space as its owner, or leaves it. The dialog stays open, saying so, if that fails. */
    fun exit() {
        val state = uiState.value
        if (!state.confirmingExit || state.exiting) return
        val deletes = state.deletes
        form.update { it.copy(exiting = true, exitFailed = false) }
        viewModelScope.launch {
            attempt(if (deletes) "Deleting space $spaceId" else "Leaving space $spaceId") {
                if (deletes) spaces.deleteSpace(spaceId) else spaces.leaveSpace(spaceId)
            }
                .onSuccess { form.update { it.copy(exiting = false, confirmingExit = false, gone = true) } }
                .onFailure { form.update { it.copy(exiting = false, exitFailed = true) } }
        }
    }
}
