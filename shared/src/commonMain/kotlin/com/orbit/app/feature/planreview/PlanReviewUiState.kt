package com.orbit.app.feature.planreview

import com.orbit.app.feature.spacehome.items.SpaceItem
import kotlinx.datetime.LocalDate

/** A proposal as the row its item gets in Tasks, and whether the user accepted it. */
data class ProposalRow(val item: SpaceItem, val accepted: Boolean)

/** The assistant's plan under review (#75): its proposals, each accepted, edited or removed. Nothing is written here. */
data class PlanReviewUiState(
    /** There is no plan to review: it was never made, or the app was closed in between. */
    val missing: Boolean = false,
    /** The space the plan is for: the one it proposes, or the one it was asked from. Unknown while it loads. */
    val spaceName: String? = null,
    val proposals: List<ProposalRow> = emptyList(),
    /** Today, for the rows' due dates. */
    val today: LocalDate? = null,
) {
    val acceptedCount: Int get() = proposals.count { it.accepted }
    val canAcceptAll: Boolean get() = proposals.any { !it.accepted }

    /** Nothing left to review: the plan is gone, or every proposal was removed. */
    val empty: Boolean get() = missing || proposals.isEmpty()
}
