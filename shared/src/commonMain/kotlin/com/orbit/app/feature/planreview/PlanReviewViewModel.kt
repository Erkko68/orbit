package com.orbit.app.feature.planreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.PlanDraft
import com.orbit.app.core.membersOrYou
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Proposal
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.spacehome.items.toSpaceItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The review board of the assistant's plan (#75), for [spaceId], or for the space the plan proposes when it is empty.
 * It reads and changes the [plan] in memory only: the members and the space's name only decorate the rows.
 */
class PlanReviewViewModel(
    private val spaceId: String,
    private val plan: PlanDraft,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val uiState: StateFlow<PlanReviewUiState> = combine(
        plan.review,
        spaces.membersOrYou(spaceId, auth).onStart { emit(emptyList()) }
            .orOnFailure("Loading the members of $spaceId", emptyList()),
        spaces.observeSpaces().onStart { emit(emptyList()) }.orOnFailure("Loading the spaces", emptyList()),
    ) { review, members, all ->
        PlanReviewUiState(
            missing = review == null,
            spaceName = review?.space?.name ?: all.firstOrNull { it.id == spaceId }?.name,
            proposals = review?.proposals.orEmpty().map { ProposalRow(toSpaceItem(it.asItem(), members), it.accepted) },
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanReviewUiState())

    /** Accepts the proposal, or takes the acceptance back. */
    fun toggleAccepted(id: String) = plan.update { review ->
        review.accept(id, review.proposals.none { it.id == id && it.accepted })
    }

    fun acceptAll() = plan.update { it.acceptAll() }
}

/** The item a proposal would create, as Tasks would show it: still open, by nobody yet. */
private fun Proposal.asItem() = Item(
    id = id,
    title = item.title,
    description = item.description,
    status = ItemStatus.Open,
    createdBy = "",
    createdAt = Instant.DISTANT_PAST,
    modules = item.modules,
)
