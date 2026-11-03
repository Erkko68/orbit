package com.orbit.app.feature.item.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Who completed an item and when. It only adds to the item screen, so a failed read shows no history. */
class ItemHistoryViewModel(
    spaceId: String,
    itemId: String,
    items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    val uiState: StateFlow<ItemHistoryUiState> = combine(
        items.observeCompletions(spaceId, itemId).orOnFailure("Loading the history of item $itemId", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
    ) { completions, members, user ->
        ItemHistoryUiState(completions.map { toItemCompletion(it, members, user?.id) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemHistoryUiState())
}

private fun toItemCompletion(completion: Completion, members: List<Member>, userId: String?): ItemCompletion {
    val index = members.indexOfFirst { it.userId == completion.completedBy }
    val member = members.getOrNull(index)
    val moment = completion.completedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    return ItemCompletion(
        id = completion.id,
        name = member?.displayName,
        isYou = completion.completedBy == userId,
        initials = initialsOf(member?.displayName.orEmpty()),
        photoUrl = member?.photoUrl,
        index = index.coerceAtLeast(0),
        date = moment.date,
        time = LocalTime(moment.hour, moment.minute),
    )
}
