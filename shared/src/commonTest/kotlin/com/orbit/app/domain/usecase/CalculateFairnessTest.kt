package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Completion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class CalculateFairnessTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private fun entry(id: String, by: String, points: Int?, at: Instant = now, reopened: Boolean = false) =
        Completion(id, "item", by, at, reopened, points)

    @Test
    fun noCompletionsOrNoMembersHasNoMeasuredShare() {
        assertEquals(
            listOf(EffortShare("a", 0, 0, 2), EffortShare("b", 0, 0, 2)),
            calculateFairness(emptyList(), listOf("b", "a"), now)
        )
        assertEquals(emptyList(), calculateFairness(listOf(entry("1", "a", 3)), emptyList(), now))
    }

    @Test
    fun usesSnapshotsAndTheActualCompleterWithinTheInclusiveThirtyDays() {
        val entries = listOf(
            entry("1", "a", 5, now - 30.days), entry("2", "b", 3),
            entry("old", "a", 5, now - 31.days), entry("future", "a", 5, now + 1.days),
            entry("reopen", "a", 5, reopened = true), entry("legacy", "b", null),
            entry("invalid", "a", 6), entry("repeat", "b", null),
            entry("recompleted", "b", 5).copy(effortCredited = false),
        )
        assertEquals(
            listOf(EffortShare("a", 5, 8, 2), EffortShare("b", 3, 8, 2)),
            calculateFairness(entries, listOf("a", "b"), now)
        )
    }

    @Test
    fun oneMemberHasTheWholeShareAndDuplicateMembershipDoesNotChangeIt() {
        assertEquals(
            listOf(EffortShare("a", 4, 4, 1)),
            calculateFairness(listOf(entry("1", "a", 4)), listOf("a", "a"), now)
        )
    }

    @Test
    fun leavingExcludesTheirPointsAndRejoiningRestoresTheRecentOnes() {
        val entries = listOf(entry("1", "a", 2), entry("2", "left", 5))
        assertEquals(listOf(EffortShare("a", 2, 2, 1)), calculateFairness(entries, listOf("a"), now))
        assertEquals(
            listOf(EffortShare("a", 2, 7, 2), EffortShare("left", 5, 7, 2)),
            calculateFairness(entries, listOf("a", "left"), now)
        )
    }
}
