package com.orbit.app.domain.usecase

/** The saved shares, in uid order. No zero shares are stored, even when cents are fewer than people. */
fun splitEqually(cents: Long, memberIds: Collection<String>): Map<String, Long> {
    require(cents > 0)
    val ids = memberIds.distinct().sorted()
    require(ids.isNotEmpty())
    val each = cents / ids.size
    val extra = cents % ids.size
    return ids.mapIndexedNotNull { index, id ->
        val share = each + if (index < extra) 1 else 0
        (id to share).takeIf { share > 0 }
    }.toMap()
}

/** Exact shares must be non-negative and sum to the typed total without overflowing Long. */
fun splitExactly(cents: Long, shares: Map<String, Long>): Map<String, Long>? {
    if (cents <= 0 || shares.isEmpty()) return null
    var remaining = cents
    for (share in shares.values) {
        if (share < 0 || share > remaining) return null
        remaining -= share
    }
    return if (remaining == 0L) shares.toSortedMapByUid() else null
}

private fun Map<String, Long>.toSortedMapByUid(): Map<String, Long> =
    keys.sorted().mapNotNull { id -> getValue(id).takeIf { it > 0 }?.let { id to it } }.toMap()

/** Above this, `cents % sum * weight` could overflow Long. Real splits (2 : 1 : 1, percentages) are far below it. */
const val MAX_WEIGHT_SUM = 1_000_000_000L

/**
 * Weights (2 : 1 : 1) as cents: each person gets the floor of their part, and the leftover cents go one each to
 * the people with a positive weight, in uid order. A zero weight owes nothing.
 */
fun splitByWeights(cents: Long, weights: Map<String, Long>): Map<String, Long>? {
    if (cents <= 0 || weights.values.any { it !in 0..MAX_WEIGHT_SUM }) return null
    val sum = weights.values.sum()
    if (sum !in 1..MAX_WEIGHT_SUM) return null
    val floors = weights.mapValues { (_, weight) -> cents / sum * weight + cents % sum * weight / sum }
    var leftover = cents - floors.values.sum()
    return weights.keys.sorted().mapNotNull { id ->
        val extra = if (leftover > 0 && weights.getValue(id) > 0) 1L.also { leftover-- } else 0L
        (floors.getValue(id) + extra).takeIf { it > 0 }?.let { id to it }
    }.toMap()
}

/** Percentages in hundredths (12,50 % is 1250). They must add up to exactly 100 %. */
fun splitByPercentages(cents: Long, hundredths: Map<String, Long>): Map<String, Long>? =
    if (hundredths.values.all { it in 0..HUNDRED_PERCENT } && hundredths.values.sum() == HUNDRED_PERCENT) {
        splitByWeights(cents, hundredths)
    } else {
        null
    }

/** 100 %, in hundredths. */
const val HUNDRED_PERCENT = 10_000L
