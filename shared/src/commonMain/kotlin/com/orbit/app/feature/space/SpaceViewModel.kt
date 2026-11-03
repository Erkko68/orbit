package com.orbit.app.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.PendingInvitation
import com.orbit.app.core.asLoad
import com.orbit.app.core.initialsOf
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SpaceViewModel(
    repository: SpaceRepository,
    auth: AuthRepository,
    pendingInvitation: PendingInvitation,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** Only this part fails and retries: the rest stays on screen. */
    private val spaceIds = repository.observeSpaces()
        .map { spaces -> spaces.map { it.id } }
        .asLoad("Loading the spaces", retries)

    val uiState: StateFlow<SpaceUiState> = combine(
        spaceIds,
        auth.session,
        pendingInvitation.code,
    ) { ids, user, waiting ->
        SpaceUiState(
            invitationWaiting = waiting != null,
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
}
