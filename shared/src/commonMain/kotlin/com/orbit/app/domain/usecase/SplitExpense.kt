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

/** Integer product/division, including its remainder, without constructing the overflowing product. */
data class ShareDivision(val whole: Long, val remainder: Long)

fun divideShare(value: Long, numerator: Long, denominator: Long): ShareDivision? {
    if (value < 0 || numerator < 0 || denominator <= 0) return null
    val addend = ShareDivision(numerator / denominator, numerator % denominator)
    var result = ShareDivision(0, 0)
    for (bit in 62 downTo 0) {
        result = result.add(result, denominator) ?: return null
        if (((value ushr bit) and 1L) != 0L) result = result.add(addend, denominator) ?: return null
    }
    return result
}

private fun ShareDivision.add(other: ShareDivision, denominator: Long): ShareDivision? {
    val carry = remainder >= denominator - other.remainder
    val fraction = if (carry) remainder - (denominator - other.remainder) else remainder + other.remainder
    if (other.whole > Long.MAX_VALUE - whole) return null
    val sum = whole + other.whole
    if (carry && sum == Long.MAX_VALUE) return null
    return ShareDivision(sum + if (carry) 1 else 0, fraction)
}

/** Positive integer weights. A sum outside Long is invalid, rather than silently overflowing. */
fun splitByWeights(cents: Long, weights: Map<String, Long>): Map<String, Long>? {
    if (cents <= 0 || weights.isEmpty() || weights.values.any { it <= 0 }) return null
    val sum = sumShares(weights.values) ?: return null
    val floors = weights.mapValues { divideShare(cents, it.value, sum)?.whole ?: return null }
    return distributeRemainder(cents, floors, weights.keys)
}

/**
 * Untouched percentages retain their original cents/total ratio; edited percentages are hundredths.
 * Both denominators stay separate so even a Long-sized anchor needs no overflowing common denominator.
 */
data class ExpensePercentages(
    val referenceTotal: Long,
    val referenceShares: Map<String, Long>,
    val hundredths: Map<String, Long> = emptyMap(),
) {
    fun progress(ids: Set<String>): ShareDivision? {
        if (referenceTotal <= 0 || ids.isEmpty()) return null
        val entered = ids.mapNotNull { hundredths[it] }
        if (entered.any { it !in 0..10_000 }) return null
        val enteredSum = sumShares(entered) ?: return null
        val retained = ids.filter { it !in hundredths }.map { referenceShares[it] ?: return null }
        val retainedSum = sumShares(retained) ?: return null
        val scaled = divideShare(retainedSum, 10_000, referenceTotal) ?: return null
        if (enteredSum > Long.MAX_VALUE - scaled.whole) return null
        return scaled.copy(whole = scaled.whole + enteredSum)
    }

    fun percentage(id: String): ShareDivision? = hundredths[id]?.let {
        if (it in 0..10_000) ShareDivision(it, 0) else null
    } ?: referenceShares[id]?.let { divideShare(it, 10_000, referenceTotal) }

    fun preview(cents: Long, ids: Set<String>): Map<String, Long>? = ids.associateWith { id ->
        val explicit = hundredths[id]
        if (explicit != null) {
            if (explicit !in 0..10_000) return null
            divideShare(cents, explicit, 10_000)?.whole ?: return null
        } else {
            divideShare(cents, referenceShares[id] ?: return null, referenceTotal)?.whole ?: return null
        }
    }

    fun split(cents: Long, ids: Set<String>): Map<String, Long>? {
        if (cents <= 0 || progress(ids) != ShareDivision(10_000, 0)) return null
        val floors = preview(cents, ids) ?: return null
        val positive = ids.filter { (hundredths[it] ?: referenceShares[it] ?: 0) > 0 }.toSet()
        return distributeRemainder(cents, floors, positive)
    }
}

private fun sumShares(values: Collection<Long>): Long? {
    var sum = 0L
    for (value in values) {
        if (value < 0 || value > Long.MAX_VALUE - sum) return null
        sum += value
    }
    return sum
}

private fun distributeRemainder(cents: Long, floors: Map<String, Long>, positive: Set<String>): Map<String, Long>? {
    val sum = sumShares(floors.values) ?: return null
    val remainder = cents - sum
    val ordered = positive.sorted()
    if (remainder < 0 || remainder > ordered.size) return null
    val extra = ordered.take(remainder.toInt()).toSet()
    return floors.mapValues { (id, value) -> value + if (id in extra) 1 else 0 }.toSortedMapByUid()
}
