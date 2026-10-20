package com.orbit.app.feature.spacehome.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.item.icon
import com.orbit.app.feature.item.label
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_unknown_member
import orbit.shared.generated.resources.module_assignment_anyone
import orbit.shared.generated.resources.module_assignment_unassigned

/** The items tab of a space: its items and the filter over them. */
class SpaceItemsViewModel(
    private val spaceId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)
    private val actionFailed = MutableStateFlow(false)
    private val submitting = MutableStateFlow(false)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    // Names only decorate the cards: a slow or failed member read must not hide the items.
    private val memberNames = flow { emitAll(spaces.observeMembers(spaceId)) }
        .map { members -> members.associate { it.userId to it.displayName } }
        .onStart { emit(emptyMap()) }
        .orOnFailure("Loading assignees for items in space $spaceId", emptyMap())

    val uiState: StateFlow<SpaceItemsUiState> = combine(
        items.observeItems(spaceId).asLoad("Loading the items of $spaceId", retries),
        filter,
        actionFailed,
        submitting,
        memberNames,
    ) { load, selected, failed, isSubmitting, names ->
        val filtered = load.valueOrNull.orEmpty().filter { selected.matches(it.preset) }
        val (open, done) = filtered.partition { it.status is ItemStatus.Open }
        SpaceItemsUiState(
            filter = selected,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            items = (open + done).map { toSpaceItem(it, names) },
            failed = failed,
            submitting = isSubmitting,
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
}

private fun toSpaceItem(item: Item, memberNames: Map<String, String>) = SpaceItem(
    id = item.id,
    title = item.title,
    label = item.preset.label,
    icon = item.preset.icon,
    done = item.status is ItemStatus.Done,
    assignmentLabel = when (val assignment = item.modules.assignment) {
        null -> null
        Assignment.Anyone -> Res.string.module_assignment_anyone
        is Assignment.Claim -> if (assignment.assigneeId == null) {
            Res.string.module_assignment_unassigned
        } else {
            Res.string.item_unknown_member
        }

        is Assignment.ToMember -> Res.string.item_unknown_member
    },
    assigneeName = item.modules.assignment?.assigneeId?.let { memberNames[it] }?.takeIf { it.isNotBlank() },
    dueDate = item.modules.schedule?.deadline,
    checklist = item.modules.checklist?.let { checklist ->
        SpaceItemChecklist(done = checklist.entries.count { it.checked }, total = checklist.entries.size)
    },
)
