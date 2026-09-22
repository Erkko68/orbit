package com.orbit.app.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SpaceViewModel(repository: SpaceRepository) : ViewModel() {
    val uiState: StateFlow<SpaceUiState> = repository.observeSpaces()
        .map(::SpaceUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceUiState())
}
