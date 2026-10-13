package com.orbit.app.feature.space.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One space's card in the list of spaces: the space and its members. Stays empty for a space the user cannot see. */
class SpaceCardViewModel(spaceId: String, repository: SpaceRepository) : ViewModel() {
    // ponytail: filters the user's spaces. Give the repository an observeSpace(id) if a card needs more than the list has.
    val uiState: StateFlow<SpaceCardUiState> = combine(
        repository.observeSpaces(),
        repository.observeMembers(spaceId),
    ) { spaces, members ->
        val space = spaces.firstOrNull { it.id == spaceId } ?: return@combine SpaceCardUiState()
        SpaceCardUiState(
            name = space.name,
            accent = space.accent,
            memberCount = space.memberIds.size,
            memberInitials = members.map { initialsOf(it.displayName) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceCardUiState())
}
