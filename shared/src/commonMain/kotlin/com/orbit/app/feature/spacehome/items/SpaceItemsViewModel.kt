package com.orbit.app.feature.spacehome.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.*
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.item.ItemFace
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val PEEK_ENTRIES = 2

/** The Tasks destination of a space: its items, the filter over them and what is the user's to do next. */
class SpaceItemsViewModel(
    private val spaceId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)
    private val actionFailed = MutableStateFlow(false)
    private val submitting = MutableStateFlow(false)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** The item the user just finished from the turn card, which stays there so they can undo it. */
    private val finishedTurn = MutableStateFlow<String?>(null)

    // Members only decorate the rows: a slow or failed read must not hide the items.
    private val memberIds = flow { emitAll(spaces.observeMembers(spaceId)) }
        .onStart { emit(emptyList()) }
        .orOnFailure("Loading assignees for items in space $spaceId", emptyList())

    private val request = combine(filter, finishedTurn, actionFailed, submitting) { filter, finished, failed, busy ->
        Request(filter, finished, failed, busy)
    }

    val uiState: StateFlow<SpaceItemsUiState> = combine(
        items.observeItems(spaceId).asLoad("Loading the items of $spaceId", retries),
        request,
        memberIds,
        auth.session,
    ) { load, request, members, user ->
        val visible = load.valueOrNull.orEmpty().filter { request.filter.matches(it, user?.id) }
        val turn = visible.firstOrNull { it.id == request.finishedTurn } ?: visible.nextTurn(user?.id)
        val (open, done) = visible.filter { it.id != turn?.id }.partition { it.status is ItemStatus.Open }
        SpaceItemsUiState(
            filter = request.filter,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            items = (open + done).map { toSpaceItem(it, members) },
            turn = turn?.let { toSpaceItem(it, members) },
            userInitials = initialsOf(user?.displayName.orEmpty()),
            userPhotoUrl = user?.photoUrl,
            today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            failed = request.failed,
            submitting = request.submitting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceItemsUiState(loading = true))

    fun selectFilter(filter: ItemFilter) {
        this.filter.value = filter
    }

    fun complete(itemId: String) = submit("Completing item $itemId in space $spaceId") {
        items.complete(spaceId, itemId)
    }

    fun reopen(itemId: String) = submit("Reopening item $itemId in space $spaceId") {
        items.reopen(spaceId, itemId)
    }

    fun toggleDone(itemId: String, done: Boolean) {
        if (done) complete(itemId) else reopen(itemId)
    }

    /** Finishes what the turn card shows and keeps it there, or undoes it once finished. */
    fun toggleTurn() {
        val turn = uiState.value.turn ?: return
        if (turn.done) {
            finishedTurn.value = null
            reopen(turn.id)
        } else {
            finishedTurn.value = turn.id
            complete(turn.id)
        }
    }

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    private fun submit(description: String, write: suspend () -> Unit) {
        if (submitting.value) return
        submitting.value = true
        actionFailed.value = false
        viewModelScope.launch {
            try {
                val result = attempt(description, write)
                actionFailed.value = result.isFailure
            } finally {
                submitting.value = false
            }
        }
    }

    private data class Request(
        val filter: ItemFilter,
        val finishedTurn: String?,
        val failed: Boolean,
        val submitting: Boolean,
    )
}

/** The open item assigned to [userId] that is due first. An item without a date comes after any with one. A checklist is finished by ticking it, so it has no turn. */
private fun List<Item>.nextTurn(userId: String?): Item? = filter {
    userId != null && it.status is ItemStatus.Open && it.modules.checklist == null && it.modules.isAssignedTo(userId)
}.minWithOrNull(compareBy(nullsLast()) { it.modules.schedule?.deadline })

private fun toSpaceItem(item: Item, members: List<Member>): SpaceItem {
    val assignment = item.modules.assignment
    val assigneeIds = assignment?.assigneeIds.orEmpty()
    val faces = members.mapIndexed { index, member -> ItemFace(member.userId, member.displayName, index, member.photoUrl) }
    return SpaceItem(
        id = item.id,
        title = item.title,
        done = item.status is ItemStatus.Done,
        summary = item.description,
        faces = if (assignment == Assignment.Anyone) faces else faces.filter { it.id in assigneeIds },
        dueDate = item.modules.schedule?.deadline,
        dueTime = item.modules.schedule?.time,
        checklist = item.modules.checklist?.let { checklist ->
            val left = checklist.entries.filterNot { it.checked }.map { it.text }
            SpaceItemChecklist(
                done = checklist.entries.count { it.checked },
                total = checklist.entries.size,
                peek = SpaceItemPeek(left.take(PEEK_ENTRIES), (left.size - PEEK_ENTRIES).coerceAtLeast(0)),
            )
        },
    )
}
