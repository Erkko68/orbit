package com.orbit.app.feature.member.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * What [userId] has and did lately in a space, on their screen. Who they are and their role come
 * from `SpaceMembersViewModel`, which already listed them; their items and completions only add to that, so a failed
 * read of those leaves them empty. What they have shows as soon as the items arrive, without waiting for the history.
 */
class MemberActivityViewModel(spaceId: String, private val userId: String, items: ItemRepository) : ViewModel() {
    private val zone = TimeZone.currentSystemDefault()

    val uiState: StateFlow<MemberActivityUiState> = combine(
        items.observeItems(spaceId).orOnFailure("Loading the items of $spaceId", emptyList()),
        items.observeMemberCompletions(spaceId, userId)
            .orOnFailure("Loading what $userId completed in $spaceId", emptyList())
            .onStart { emit(emptyList()) },
    ) { all, completions ->
        MemberActivityUiState(
            today = Clock.System.todayIn(zone),
            has = all.filter { it.status is ItemStatus.Open && it.modules.isAssignedTo(userId) }
                .sortedWith(compareBy(nullsLast()) { it: Item -> it.modules.schedule?.deadline })
                .map { MemberItem(it.id, it.title, it.modules.schedule?.deadline) },
            lately = completions.filterNot { it.reopened }.mapNotNull { toMemberCompletion(it, all) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemberActivityUiState(loading = true))

    /** A completion of an item since deleted has nothing to show. */
    private fun toMemberCompletion(completion: Completion, items: List<Item>): MemberCompletion? {
        val item = items.firstOrNull { it.id == completion.itemId } ?: return null
        val moment = completion.completedAt.toLocalDateTime(zone)
        return MemberCompletion(completion.id, item.id, item.title, moment.date, LocalTime(moment.hour, moment.minute))
    }
}
