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
