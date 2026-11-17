package com.orbit.app.feature.member.fairness

import com.orbit.app.domain.usecase.EffortShare

data class FairnessUiState(
    val loading: Boolean = true,
    val historyFailed: Boolean = false,
    val membersFailed: Boolean = false,
    val shares: List<EffortShare> = emptyList(),
) {
    val loadFailed: Boolean get() = historyFailed || membersFailed
    val hasPoints: Boolean get() = shares.any { it.totalPoints > 0 }
    fun share(memberId: String): EffortShare? = shares.firstOrNull { it.memberId == memberId }
}

/** Display-only rounding; exact point totals remain available to the calculation and future callers. */
internal val EffortShare.roundedPercentage: Int
    get() = if (totalPoints == 0L) 0 else ((points * 100 + totalPoints / 2) / totalPoints).toInt()

internal val EffortShare.roundedEqualPercentage: Int
    get() = if (memberCount == 0) 0 else (100 + memberCount / 2) / memberCount
