package com.orbit.app.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceViewModel(private val repository: SpaceRepository) : ViewModel() {
    /** The create form. Its `spaceIds` stay empty: [uiState] fills them from the repository. */
    private val form = MutableStateFlow(SpaceUiState())

    val uiState: StateFlow<SpaceUiState> = combine(repository.observeSpaces(), form) { spaces, form ->
        form.copy(spaceIds = spaces.map { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceUiState())

    fun onNewSpaceNameChange(name: String) = form.update { it.copy(newSpaceName = name, createFailed = false) }

    /** Creates a space owned by the signed-in user. Accent picking comes with the full create flow (#22). */
    fun createSpace() {
        val current = form.value
        if (!current.canCreate) return
        form.update { it.copy(creating = true, createFailed = false) }
        viewModelScope.launch {
            try {
                repository.createSpace(current.newSpaceName.trim(), accent = 0)
                form.value = SpaceUiState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Creating a space failed" }
                form.update { it.copy(creating = false, createFailed = true) }
            }
        }
    }
}
