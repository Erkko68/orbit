package com.orbit.app.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.initialsOf
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceViewModel(private val repository: SpaceRepository, auth: AuthRepository) : ViewModel() {
    /** The create dialog. Its user and spaces stay empty: [uiState] fills them from the repositories. */
    private val form = MutableStateFlow(SpaceUiState())

    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** Only this part fails and retries: the rest stays on screen. */
    private val spaceIds = repository.observeSpaces()
        .map { spaces -> spaces.map { it.id } }
        .asLoad("Loading the spaces", retries)

    val uiState: StateFlow<SpaceUiState> = combine(spaceIds, auth.session, form) { ids, user, form ->
        form.copy(
            loading = ids == Load.Loading,
            loadFailed = ids == Load.Failed,
            userInitials = initialsOf(user?.displayName.orEmpty()),
            userPhotoUrl = user?.photoUrl,
            spaceIds = ids.valueOrNull.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun openCreate() = form.update { it.copy(createOpen = true) }

    /** Closes the create dialog and forgets what was typed in it. */
    fun dismissCreate() {
        form.value = SpaceUiState()
    }

    fun onNewSpaceNameChange(name: String) = form.update { it.copy(newSpaceName = name, createFailed = false) }

    /** Creates a space owned by the signed-in user and closes the dialog. */
    fun createSpace() {
        val current = form.value
        if (!current.canCreate) return
        form.update { it.copy(creating = true, createFailed = false) }
        viewModelScope.launch {
            attempt("Creating a space") { repository.createSpace(current.newSpaceName.trim(), accent = 0) }
                .onSuccess { form.value = SpaceUiState() }
                .onFailure { form.update { it.copy(creating = false, createFailed = true) } }
        }
    }
}
