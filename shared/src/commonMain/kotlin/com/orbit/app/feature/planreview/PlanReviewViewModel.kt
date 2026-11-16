package com.orbit.app.feature.planreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.PlanDraft
import com.orbit.app.core.attempt
import com.orbit.app.core.membersOrYou
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Proposal
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.PlanRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.spacehome.items.toSpaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The review board of the assistant's plan (#75), for [spaceId], or for the space the plan proposes when it is empty.
 * It changes the [plan] in memory only, until the user confirms the summary: then [plans] saves the accepted proposals
 * (#76). The members and the space's name only decorate the rows.
 */
class PlanReviewViewModel(
    private val spaceId: String,
    private val plan: PlanDraft,
    private val plans: PlanRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /** The confirmation's part of the state: the summary, the save and its outcome. */
    private val confirmation = MutableStateFlow(PlanReviewUiState())

    val uiState: StateFlow<PlanReviewUiState> = combine(
        plan.review,
        spaces.membersOrYou(spaceId, auth).onStart { emit(emptyList()) }
            .orOnFailure("Loading the members of $spaceId", emptyList()),
        spaces.observeSpaces().onStart { emit(emptyList()) }.orOnFailure("Loading the spaces", emptyList()),
        confirmation,
    ) { review, members, all, confirm ->
        confirm.copy(
            missing = review == null,
            spaceName = review?.space?.name ?: all.firstOrNull { it.id == spaceId }?.name,
            proposals = review?.proposals.orEmpty().map { ProposalRow(toSpaceItem(it.asItem(), members), it.accepted) },
            today = today,
            newSpace = spaceId.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanReviewUiState())

    /** Accepts the proposal, or takes the acceptance back. */
    fun toggleAccepted(id: String) = plan.update { review ->
        review.accept(id, review.proposals.none { it.id == id && it.accepted })
    }

    fun acceptAll() = plan.update { it.acceptAll() }

    /** Shows the summary of what confirming creates. */
    fun openConfirm() {
        if (plan.review.value?.accepted.orEmpty().isEmpty()) return
        confirmation.update { it.copy(confirming = true, saveFailed = false) }
    }

    fun closeConfirm() {
        if (!confirmation.value.saving) confirmation.update { it.copy(confirming = false, saveFailed = false) }
    }

    /** Saves the accepted proposals, and the new space if the plan makes one, all at once. */
    fun confirm() {
        val review = plan.review.value ?: return
        val current = confirmation.value
        if (review.accepted.isEmpty() || current.saving || current.savedSpaceId != null) return
        confirmation.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            attempt("Confirming the plan") { plans.confirm(spaceId, review) }
                .onSuccess { id -> confirmation.update { it.copy(saving = false, confirming = false, savedSpaceId = id) } }
                .onFailure { confirmation.update { it.copy(saving = false, saveFailed = true) } }
        }
    }
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
