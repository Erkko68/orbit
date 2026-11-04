package com.orbit.app.feature.item.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * What happened to an item: who completed or reopened it and when, and who created it. It only adds to the item screen,
 * so a failed read shows no history.
 */
class ItemHistoryViewModel(
    spaceId: String,
    itemId: String,
    items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<ItemHistoryUiState> = combine(
        items.observeCompletions(spaceId, itemId).orOnFailure("Loading the history of item $itemId", emptyList()),
        items.observeItems(spaceId).map { all -> all.firstOrNull { it.id == itemId } }
            .orOnFailure("Loading item $itemId for its history", null),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
    ) { completions, item, members, user ->
        val entry = { id: String, action: ItemHistoryAction, by: String, at: Instant ->
            toEntry(id, action, by, at, members, user?.id)
        }
        val done = completions.map {
            entry(it.id, if (it.reopened) ItemHistoryAction.REOPENED else ItemHistoryAction.COMPLETED, it.completedBy, it.completedAt)
        }
        val created = item?.let { entry("created", ItemHistoryAction.CREATED, it.createdBy, it.createdAt) }
        ItemHistoryUiState(done + listOfNotNull(created))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemHistoryUiState())
}

private fun toEntry(
    id: String,
    action: ItemHistoryAction,
    by: String,
    at: Instant,
    members: List<Member>,
    userId: String?,
): ItemHistoryEntry {
    val index = members.indexOfFirst { it.userId == by }
    val member = members.getOrNull(index)
    val moment = at.toLocalDateTime(TimeZone.currentSystemDefault())
    return ItemHistoryEntry(
        id = id,
        action = action,
        name = member?.displayName,
        isYou = by == userId,
        initials = initialsOf(member?.displayName.orEmpty()),
        photoUrl = member?.photoUrl,
        index = index.coerceAtLeast(0),
        date = moment.date,
        time = LocalTime(moment.hour, moment.minute),
    )
}
