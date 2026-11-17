package com.orbit.app.feature.member.fairness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.calculateFairness
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/** One comparison per space, shared by the settings' member rows or the member page. */
class FairnessViewModel(spaceId: String, items: ItemRepository, spaces: SpaceRepository) : ViewModel() {
    private val completionRetries = Channel<Unit>(Channel.CONFLATED)
    private val memberRetries = Channel<Unit>(Channel.CONFLATED)
    private val since = Clock.System.now() - 30.days
    private val now = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    val uiState = combine(
        items.observeRecentCompletions(spaceId, since).asLoad("Loading effort in $spaceId", completionRetries),
        spaces.observeMembers(spaceId).asLoad("Loading effort members in $spaceId", memberRetries),
        now,
    ) { completions, members, _ ->
        FairnessUiState(
            loading = completions == Load.Loading || members == Load.Loading,
            historyFailed = completions == Load.Failed,
            membersFailed = members == Load.Failed,
            shares = if (completions is Load.Loaded && members is Load.Loaded) {
                calculateFairness(
                    completions.valueOrNull.orEmpty(),
                    members.valueOrNull.orEmpty().map { it.userId },
                    Clock.System.now()
                )
            } else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FairnessUiState())

    fun retry() {
        if (!uiState.value.loadFailed) return
        if (uiState.value.historyFailed) completionRetries.trySend(Unit)
        if (uiState.value.membersFailed) memberRetries.trySend(Unit)
    }
}
