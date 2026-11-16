package com.orbit.app.feature.member.away

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * The signed-in user's away period in a space, for the Away row on their own member screen: a sheet with the first and
 * last day, which Save writes and Clear removes. One period at a time (#48).
 */
class AwayViewModel(private val spaceId: String, private val userId: String, private val spaces: SpaceRepository) :
    ViewModel() {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** What the sheet holds: whether it is open, its days and the write in progress. */
    private val edit = MutableStateFlow(Edit())

    val uiState: StateFlow<AwayUiState> = combine(
        spaces.observeMembers(spaceId).asLoad("Loading the away period of $userId in $spaceId", retries),
        edit,
    ) { load, edit ->
        val member = load.valueOrNull?.firstOrNull { it.userId == userId }
        val until = member?.awayUntilFrom(today)
        AwayUiState(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            from = member?.awayFrom?.takeIf { until != null },
            until = until,
            editing = edit.open,
            draftFrom = edit.from,
            draftUntil = edit.until,
            picking = edit.picking,
            busy = edit.busy,
            failed = edit.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AwayUiState())

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Opens the sheet on the saved period, or on today while there is none. */
    fun open() {
        val state = uiState.value
        edit.value = Edit(open = true, from = state.from ?: today, until = state.until ?: today)
    }

    fun dismiss() {
        if (!edit.value.busy) edit.value = Edit()
    }

    fun pick(end: AwayEnd) = edit.update { if (it.busy) it else it.copy(picking = end, failed = false) }

    fun dismissPicker() = edit.update { it.copy(picking = null) }

    /** Takes the day the picker confirmed. A first day after the last one moves the last one along with it. */
    fun pickDate(utcMillis: Long) = edit.update {
        val day = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
        when (it.picking) {
            AwayEnd.FROM -> it.copy(from = day, until = maxOf(day, it.until ?: day), picking = null, failed = false)
            AwayEnd.UNTIL -> it.copy(until = day, picking = null, failed = false)
            null -> it
        }
    }

    /** Saves the days in the sheet. The sheet stays open, with the failure said, when the write fails. */
    fun save() {
        val state = uiState.value
        val from = state.draftFrom
        val until = state.draftUntil
        if (!state.canSave || from == null || until == null) return
        submit("Setting $userId away in $spaceId") { spaces.setAway(spaceId, from, until) }
    }

    /** Removes the period. */
    fun clear() {
        if (!uiState.value.isAway || uiState.value.busy) return
        submit("Clearing the away period of $userId in $spaceId") { spaces.clearAway(spaceId) }
    }

    private fun submit(what: String, write: suspend () -> Unit) {
        edit.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            val result = attempt(what) { write() }
            edit.update { if (result.isSuccess) Edit() else it.copy(busy = false, failed = true) }
        }
    }

    private data class Edit(
        val open: Boolean = false,
        val from: LocalDate? = null,
        val until: LocalDate? = null,
        val picking: AwayEnd? = null,
        val busy: Boolean = false,
        val failed: Boolean = false,
    )
}
