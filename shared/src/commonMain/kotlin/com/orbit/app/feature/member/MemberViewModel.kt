package com.orbit.app.feature.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.initialsOf
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.item.label
import kotlinx.coroutines.channels.Channel
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
 * The page of [userId] in a space. The members are what it is about; their items, completions and the space's name only
 * decorate it, so a failed read of those leaves them empty. Changing the role goes through `SpaceMembersViewModel`.
 */
class MemberViewModel(
    private val spaceId: String,
    private val userId: String,
    items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)
    private val zone = TimeZone.currentSystemDefault()

    private val activity = combine(
        items.observeItems(spaceId).orOnFailure("Loading the items of $spaceId", emptyList()).onStart { emit(emptyList()) },
        items.observeMemberCompletions(spaceId, userId)
            .orOnFailure("Loading what $userId completed in $spaceId", emptyList())
            .onStart { emit(emptyList()) },
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()).onStart { emit(emptyList()) },
    ) { all, completions, spaceList -> Activity(all, completions, spaceList.firstOrNull { it.id == spaceId }?.name.orEmpty()) }

    val uiState: StateFlow<MemberUiState> = combine(
        spaces.observeMembers(spaceId).asLoad("Loading the members of $spaceId", retries),
        auth.session,
        activity,
    ) { load, user, activity ->
        val members = load.valueOrNull.orEmpty()
        val index = members.indexOfFirst { it.userId == userId }
        val member = members.getOrNull(index)
        MemberUiState(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            notFound = load is Load.Loaded && member == null,
            name = member?.displayName.orEmpty(),
            isYou = userId == user?.id,
            initials = initialsOf(member?.displayName.orEmpty()),
            photoUrl = member?.photoUrl,
            index = index.coerceAtLeast(0),
            role = member?.role?.label,
            spaceName = activity.spaceName,
            today = Clock.System.todayIn(zone),
            has = activity.items.filter { it.status is ItemStatus.Open && it.modules.isAssignedTo(userId) }
                .sortedWith(compareBy(nullsLast()) { it: Item -> it.modules.schedule?.deadline })
                .map { MemberItem(it.id, it.title, it.modules.schedule?.deadline) },
            lately = activity.completions.mapNotNull { toMemberCompletion(it, activity.items) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemberUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** A completion of an item since deleted has nothing to show. */
    private fun toMemberCompletion(completion: Completion, items: List<Item>): MemberCompletion? {
        val item = items.firstOrNull { it.id == completion.itemId } ?: return null
        val moment = completion.completedAt.toLocalDateTime(zone)
        return MemberCompletion(completion.id, item.id, item.title, moment.date, LocalTime(moment.hour, moment.minute))
    }

    private class Activity(val items: List<Item>, val completions: List<Completion>, val spaceName: String)
}
