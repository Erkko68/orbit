package com.orbit.app.feature.space.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One space's card in the list of spaces. Stays empty for a space the user cannot see. */
class SpaceCardViewModel(spaceId: String, repository: SpaceRepository) : ViewModel() {
    // ponytail: filters the user's spaces. Give the repository an observeSpace(id) if a card needs more than the list has.
    val uiState: StateFlow<SpaceCardUiState> = repository.observeSpaces()
        .map { spaces ->
            spaces.firstOrNull { it.id == spaceId }
                ?.let { SpaceCardUiState(it.name, it.accent, it.memberIds.size) }
                ?: SpaceCardUiState()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceCardUiState())
}
