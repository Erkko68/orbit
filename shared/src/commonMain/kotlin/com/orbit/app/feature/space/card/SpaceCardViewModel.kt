package com.orbit.app.feature.space.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.isDueFor
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * One space's card in the list of spaces: the space, its members and how things stand in it. Stays empty for a space the
 * user cannot see. The list shows when the spaces fail to load, so a failure here only leaves the card bare.
 */
class SpaceCardViewModel(
    spaceId: String,
    repository: SpaceRepository,
    items: ItemRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val loadedItems: Flow<List<Item>?> = items.observeItems(spaceId)
        .map<List<Item>, List<Item>?> { it }
        .onStart { emit(null) }
        .orOnFailure("Loading the items of $spaceId", null)

    // ponytail: filters the user's spaces. Give the repository an observeSpace(id) if a card needs more than the list has.
    val uiState: StateFlow<SpaceCardUiState> = combine(
        repository.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        repository.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        loadedItems,
        auth.session,
    ) { spaces, members, loaded, user ->
        val space = spaces.firstOrNull { it.id == spaceId } ?: return@combine SpaceCardUiState()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        SpaceCardUiState(
            name = space.name,
            accent = space.accent,
            memberCount = space.memberIds.size,
            // A member's photo is copied when they join, so the user's own is taken from their account, which is current.
            faces = members.map { member ->
                val photoUrl = if (member.userId == user?.id) user.photoUrl ?: member.photoUrl else member.photoUrl
                SpaceCardFace(member.userId, member.displayName, initialsOf(member.displayName), photoUrl)
            },
            dueToday = loaded?.count { item -> user != null && item.isDueFor(user.id, today) },
            openCount = loaded?.count { it.status is ItemStatus.Open },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceCardUiState())
}
