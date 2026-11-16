package com.orbit.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanReviewTest {
    private val review = PlanReview.of(
        Plan(SpaceProposal("Flat 4B", accent = 1), listOf(NewItem("Bins"), NewItem("Groceries"))),
    )

    @Test
    fun startsWithEveryProposalStillToDecide() {
        assertEquals(listOf("proposal-0", "proposal-1"), review.proposals.map { it.id })
        assertEquals(SpaceProposal("Flat 4B", accent = 1), review.space)
        assertTrue(review.accepted.isEmpty())
    }

    @Test
    fun acceptsOneAndTakesItBack() {
        val accepted = review.accept("proposal-1", true)
        assertEquals(listOf("proposal-1"), accepted.accepted.map { it.id })
        assertTrue(accepted.accept("proposal-1", false).accepted.isEmpty())
    }

    @Test
    fun acceptAllAcceptsTheRest() {
        assertEquals(2, review.accept("proposal-0", true).acceptAll().accepted.size)
    }

    @Test
    fun anEditReplacesTheItemAndAcceptsIt() {
        val edited = review.edit("proposal-0", NewItem("Take the bins out"))
        assertEquals(listOf(NewItem("Take the bins out")), edited.accepted.map { it.item })
        assertEquals(NewItem("Groceries"), edited.proposals[1].item)
    }

    @Test
    fun removingTakesItOutOfThePlan() {
        assertEquals(listOf("proposal-1"), review.remove("proposal-0").proposals.map { it.id })
    }
}
