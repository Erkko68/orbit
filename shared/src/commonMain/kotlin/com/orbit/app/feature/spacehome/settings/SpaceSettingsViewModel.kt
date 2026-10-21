package com.orbit.app.feature.spacehome.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The settings of a space: an owner or admin changes its name and colour. */
class SpaceSettingsViewModel(
    private val spaceId: String,
    private val spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    /** What is being edited while the dialog is open, null while it is closed. */
    private val edit = MutableStateFlow<Edit?>(null)

    private val space: Flow<Space?> = spaces.observeSpaces()
        .orOnFailure("Loading the spaces", emptyList())
        .map { all -> all.firstOrNull { it.id == spaceId } }

    /** Members who cannot be read cannot be checked, so nobody can edit until they load. */
    private val canEdit: Flow<Boolean> = combine(
        auth.session,
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
    ) { user, members ->
        members.firstOrNull { it.userId == user?.id }?.role?.managesSpace == true
    }.distinctUntilChanged()

    val uiState: StateFlow<SpaceSettingsUiState> = combine(canEdit, edit) { can, edit ->
        SpaceSettingsUiState(
            canEdit = can,
            open = edit != null,
            name = edit?.name.orEmpty(),
            accent = edit?.accent ?: 0,
            saving = edit?.saving == true,
            failed = edit?.failed == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceSettingsUiState())

    /** Opens the dialog with the name and colour the space has now. */
    fun open() {
        viewModelScope.launch {
            val current = space.first() ?: return@launch
            edit.value = Edit(current.name, current.accent)
        }
    }

    fun dismiss() {
        edit.value = null
    }

    fun onNameChange(name: String) = edit.update { it?.copy(name = name, failed = false) }

    fun onAccentChange(accent: Int) = edit.update { it?.copy(accent = accent, failed = false) }

    /** Saves what changed, and only that, then closes the dialog. It stays open, saying so, if that fails. */
    fun save() {
        val editing = edit.value ?: return
        if (editing.saving || editing.name.isBlank()) return
        viewModelScope.launch {
            val current = space.first() ?: return@launch
            val name = editing.name.trim().takeIf { it != current.name }
            val accent = editing.accent.takeIf { it != current.accent }
            if (name == null && accent == null) {
                edit.value = null
                return@launch
            }
            edit.update { it?.copy(saving = true, failed = false) }
            attempt("Saving the settings of space $spaceId") { spaces.updateSpace(spaceId, name, accent) }
                .onSuccess { edit.value = null }
                .onFailure { edit.update { it?.copy(saving = false, failed = true) } }
        }
    }

    private data class Edit(val name: String, val accent: Int, val saving: Boolean = false, val failed: Boolean = false)
}
