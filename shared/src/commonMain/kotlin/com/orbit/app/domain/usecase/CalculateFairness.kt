package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Completion
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Exact inputs for a member's share; rounding belongs only to the displayed percentage. */
data class EffortShare(val memberId: String, val points: Long, val totalPoints: Long, val memberCount: Int)

/**
 * Current members' contributions in the inclusive interval [now - 30 days, now]. Former members' entries
 * remain in history but do not contribute to this comparison. Rejoining restores still-recent contributions.
 * No points means no measured share, rather than evidence of unfairness.
 */
fun calculateFairness(completions: List<Completion>, memberIds: Collection<String>, now: Instant): List<EffortShare> {
    val members = memberIds.toSortedSet()
    val since = now - 30.days
    val points = completions.asSequence()
        .filter { !it.reopened && it.effortCredited && it.completedBy in members && it.completedAt in since..now && it.effortPoints in 1..5 }
        .groupBy { it.completedBy }
        .mapValues { (_, entries) -> entries.sumOf { it.effortPoints!!.toLong() } }
    val total = points.values.sum()
    return members.map { EffortShare(it, points[it] ?: 0L, total, members.size) }
}
