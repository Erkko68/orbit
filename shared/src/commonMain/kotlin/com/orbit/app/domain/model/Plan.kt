package com.orbit.app.domain.model

/**
 * What the assistant proposes at the end of an interview (#64): the new [space] when the interview was for one, and
 * the [items] to add. Only proposals that pass the same checks as manual input get here (#65), and nothing is saved
 * until the user confirms it (#76).
 */
data class Plan(val space: SpaceProposal?, val items: List<NewItem>)

/** A new space: its [name] and [accent], an index into the space colours as a [Space] stores it. */
data class SpaceProposal(val name: String, val accent: Int)

/** How many accents a space may take: the entries of `OrbitTheme.colors.spaceAccents`. */
const val SPACE_ACCENT_COUNT = 6

/** One item of a [Plan] under review (#75): what it would create, and whether the user accepted it yet. */
data class Proposal(val id: String, val item: NewItem, val accepted: Boolean = false)

/**
 * A [Plan] while the user goes through it: each proposal accepted, edited or removed. Nothing here is written: only
 * confirming it saves the accepted ones (#76).
 */
data class PlanReview(val space: SpaceProposal?, val proposals: List<Proposal>) {
    val accepted: List<Proposal> get() = proposals.filter { it.accepted }

    fun accept(id: String, accepted: Boolean) = copy(
        proposals = proposals.map { if (it.id == id) it.copy(accepted = accepted) else it },
    )

    fun acceptAll() = copy(proposals = proposals.map { it.copy(accepted = true) })

    /** The user edited it: the edit is what they want, so it counts as accepted. */
    fun edit(id: String, item: NewItem) = copy(
        proposals = proposals.map { if (it.id == id) it.copy(item = item, accepted = true) else it },
    )

    fun remove(id: String) = copy(proposals = proposals.filterNot { it.id == id })

    companion object {
        fun of(plan: Plan) =
            PlanReview(plan.space, plan.items.mapIndexed { index, item -> Proposal("proposal-$index", item) })
    }
}
