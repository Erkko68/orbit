package com.orbit.app.feature.spacehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.isDueFor
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.item.ItemFace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class SpaceHomeViewModel(
    private val spaceId: String,
    spaces: SpaceRepository,
    items: ItemRepository,
    lastSpace: LastSpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    /** The destination. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(SpaceHomeUiState())

    init {
        // The app opens into the space opened last.
        lastSpace.remember(spaceId)
    }

    /** The header only decorates: the items show their own failures, so here a failure says nothing rather than "all clear". */
    private val loadedItems: Flow<List<Item>?> = items.observeItems(spaceId)
        .map<List<Item>, List<Item>?> { it }
        .onStart { emit(null) }
        .orOnFailure("Loading the items of $spaceId", null)

    val uiState: StateFlow<SpaceHomeUiState> = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        loadedItems,
        auth.session,
        form,
    ) { allSpaces, members, loaded, user, form ->
        val space = allSpaces.firstOrNull { it.id == spaceId }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        form.copy(
            name = space?.name.orEmpty(),
            userInitials = initialsOf(user?.displayName.orEmpty()),
            userPhotoUrl = user?.photoUrl,
            faces = members.mapIndexed { index, member -> ItemFace(member.userId, member.displayName, index, member.photoUrl) },
            today = today,
            headline = loaded?.let { headlineOf(it, user?.id, today) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceHomeUiState())

    fun selectTab(tab: SpaceHomeTab) = form.update { it.copy(tab = tab) }

    fun openAddMenu() = form.update { it.copy(addMenuOpen = true) }

    fun closeAddMenu() = form.update { it.copy(addMenuOpen = false) }
}

private fun headlineOf(items: List<Item>, userId: String?, today: LocalDate): SpaceHeadline {
    val due = items.count { userId != null && it.isDueFor(userId, today) }
    return when {
        items.isEmpty() -> SpaceHeadline.Empty
        due == 0 -> SpaceHeadline.AllClear
        else -> SpaceHeadline.Due(due)
    }
}
