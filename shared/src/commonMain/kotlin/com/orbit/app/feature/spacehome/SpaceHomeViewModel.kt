package com.orbit.app.feature.spacehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.defaultModules
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class SpaceHomeViewModel(
    private val spaceId: String,
    spaces: SpaceRepository,
    private val items: ItemRepository,
    lastSpace: LastSpaceRepository,
) : ViewModel() {
    /** The tab and the "New item" dialog. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(SpaceHomeUiState())

    init {
        // The app opens into the space opened last.
        lastSpace.remember(spaceId)
    }

    /** The header only decorates: the tabs show when their own reads fail, so here a failure reads as empty. */
    val uiState: StateFlow<SpaceHomeUiState> = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        items.observeItems(spaceId).orOnFailure("Loading the items of $spaceId", emptyList()),
        form,
    ) { allSpaces, members, spaceItems, form ->
        form.copy(
            name = allSpaces.firstOrNull { it.id == spaceId }?.name.orEmpty(),
            memberInitials = members.map { initialsOf(it.displayName) },
            openTasks = spaceItems.count { it.status == ItemStatus.Open && it.preset in TASK_PRESETS },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceHomeUiState())

    fun selectTab(tab: SpaceHomeTab) = form.update { it.copy(tab = tab) }

    fun openNewItem() = form.update { it.copy(newItemOpen = true) }

    /** Closes the "New item" dialog and forgets what was picked and typed in it. */
    fun dismissNewItem() = form.update { it.closedNewItem() }

    fun selectNewItemPreset(preset: Preset) = form.update { it.copy(newItemPreset = preset, createFailed = false) }

    fun onNewItemTitleChange(title: String) = form.update { it.copy(newItemTitle = title, createFailed = false) }

    /** Creates the item with its preset's default modules, closes the dialog and asks the screen to open it. */
    fun createItem() {
        val current = form.value
        if (!current.canCreate) return
        form.update { it.copy(creating = true, createFailed = false) }
        val item = NewItem(
            title = current.newItemTitle.trim(),
            preset = current.newItemPreset,
            modules = current.newItemPreset.defaultModules(Clock.System.todayIn(TimeZone.currentSystemDefault())),
        )
        viewModelScope.launch {
            attempt("Creating an item in space $spaceId") { items.create(spaceId, item) }
                .onSuccess { id -> form.update { it.closedNewItem().copy(createdItemId = id) } }
                .onFailure { form.update { it.copy(creating = false, createFailed = true) } }
        }
    }

    /** The screen opened [SpaceHomeUiState.createdItemId]. */
    fun onCreatedItemOpened() = form.update { it.copy(createdItemId = null) }
}

private fun SpaceHomeUiState.closedNewItem() = copy(
    newItemOpen = false,
    newItemPreset = Preset.TASK,
    newItemTitle = "",
    creating = false,
    createFailed = false,
)

/** Items without a preset count as tasks too. */
private val TASK_PRESETS = setOf(Preset.TASK, null)
