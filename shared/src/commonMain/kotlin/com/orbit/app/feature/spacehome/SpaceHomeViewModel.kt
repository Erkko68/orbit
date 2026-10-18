package com.orbit.app.feature.spacehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SpaceHomeViewModel(
    spaceId: String,
    spaces: SpaceRepository,
    items: ItemRepository,
    lastSpace: LastSpaceRepository,
) : ViewModel() {
    private val tab = MutableStateFlow(SpaceHomeTab.ITEMS)

    init {
        // The app opens into the space opened last.
        lastSpace.remember(spaceId)
    }

    val uiState: StateFlow<SpaceHomeUiState> = combine(
        spaces.observeSpaces(),
        spaces.observeMembers(spaceId),
        items.observeItems(spaceId),
        tab,
    ) { allSpaces, members, spaceItems, tab ->
        SpaceHomeUiState(
            name = allSpaces.firstOrNull { it.id == spaceId }?.name.orEmpty(),
            memberInitials = members.map { initialsOf(it.displayName) },
            tab = tab,
            openTasks = spaceItems.count { it.status == ItemStatus.Open && it.preset in TASK_PRESETS },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceHomeUiState())

    fun selectTab(tab: SpaceHomeTab) {
        this.tab.value = tab
    }
}

/** Items without a preset count as tasks too. */
private val TASK_PRESETS = setOf(Preset.TASK, null)
