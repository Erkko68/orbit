package com.orbit.app.feature.spacehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.defaultModules
import com.orbit.app.domain.repository.AuthRepository
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
    auth: AuthRepository,
) : ViewModel() {
    /** The tab and the "New item" dialog. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(SpaceHomeUiState())

    init {
        // The app opens into the space opened last.
        lastSpace.remember(spaceId)
    }

    /** The header only decorates: the member row and items show their own failures, so here a failure reads as empty. */
    val uiState: StateFlow<SpaceHomeUiState> = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
        form,
    ) { allSpaces, members, user, form ->
        form.copy(
            name = allSpaces.firstOrNull { it.id == spaceId }?.name.orEmpty(),
            memberCount = members.size,
            userInitials = initialsOf(user?.displayName.orEmpty()),
            userPhotoUrl = user?.photoUrl,
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
                .onSuccess { id ->
                    val reminds = item.modules.reminder != null
                    form.update { it.closedNewItem().copy(createdItemId = id, askNotifications = reminds) }
                }
                .onFailure { form.update { it.copy(creating = false, createFailed = true) } }
        }
    }

    /** The user allowed notifications, or not: the new item opens either way. */
    fun onNotificationsAnswered() = form.update { it.copy(askNotifications = false) }

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
