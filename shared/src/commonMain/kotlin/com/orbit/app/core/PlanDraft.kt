package com.orbit.app.core

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.PlanReview
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The plan the assistant proposed, from the interview to the review board and the item screen that edits a proposal
 * (#75), and on to the confirmation (#76). In memory only: nothing is written before the user confirms.
 */
// ponytail: lost if the OS ends the app mid-review; the interview can be run again. Persist it if users hit that.
class PlanDraft {
    private val state = MutableStateFlow<PlanReview?>(null)

    val review: StateFlow<PlanReview?> = state

    fun propose(plan: Plan) {
        state.value = PlanReview.of(plan)
    }

    fun update(change: (PlanReview) -> PlanReview) = state.update { it?.let(change) }
}

/**
 * The members of [spaceId], or for a plan's space not created yet (an empty id) only the user, who will own it (#76).
 * Throws as [SpaceRepository.observeMembers] does.
 */
internal fun SpaceRepository.membersOrYou(spaceId: String, auth: AuthRepository): Flow<List<Member>> =
    if (spaceId.isNotEmpty()) {
        observeMembers(spaceId)
    } else {
        auth.session.map { user -> listOfNotNull(user?.let { Member(it.id, it.displayName, it.photoUrl, Role.OWNER) }) }
    }
