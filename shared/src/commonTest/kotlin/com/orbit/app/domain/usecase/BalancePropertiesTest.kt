package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.ExpenseKind
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BalancePropertiesTest {
    private fun expense(id: Int, payer: String, split: Map<String, Long>, kind: ExpenseKind = ExpenseKind.EXPENSE) =
        Expense(
            id.toString(), "Ledger entry", payer, split, LocalDate(2026, 11, 3), payer,
            Instant.fromEpochSeconds(id.toLong()), kind
        )

    @Test
    fun randomLedgersConserveMoneyAndSuggestedPaymentsSettleEveryone() {
        repeat(500) { seed ->
            val random = Random(seed)
            val allMembers = List(random.nextInt(1, 9)) { "member-$it" }
            val currentMembers = allMembers.shuffled(random).take(random.nextInt(allMembers.size + 1))
            val ledger = List(random.nextInt(36)) { id ->
                val payer = allMembers.random(random)
                val kind = if (random.nextBoolean()) ExpenseKind.EXPENSE else ExpenseKind.SETTLEMENT
                val recipients = if (kind == ExpenseKind.SETTLEMENT) listOf(allMembers.random(random))
                else allMembers.shuffled(random).take(random.nextInt(1, allMembers.size + 1))
                expense(id, payer, recipients.associateWith { random.nextLong(1, 5001) }, kind)
            }
            val result = calculateBalances(ledger, currentMembers)
            val context = "seed=$seed"
            val expected = allMembers.associateWith { member ->
                ledger.filter { it.paidBy == member }.sumOf { it.split.values.sum() } -
                        ledger.sumOf { it.split[member] ?: 0L }
            }.filter { (member, cents) -> member in currentMembers || cents != 0L }
            assertEquals(expected, result.members.associate { it.memberId to it.cents }, context)
            assertEquals(0L, result.members.sumOf { it.cents }, context)
            assertTrue(result.payments.size <= (result.members.size - 1).coerceAtLeast(0), context)
            assertTrue(result.payments.all { payment ->
                payment.cents > 0 && payment.paidBy != payment.paidTo &&
                        expected.getValue(payment.paidBy) < 0 && expected.getValue(payment.paidTo) > 0
            }, context)

            val settlements = result.payments.mapIndexed { id, payment ->
                expense(
                    ledger.size + id,
                    payment.paidBy,
                    mapOf(payment.paidTo to payment.cents),
                    ExpenseKind.SETTLEMENT
                )
            }
            val settled = calculateBalances(ledger + settlements, currentMembers)
            assertEquals(currentMembers.toSet(), settled.members.map { it.memberId }.toSet(), context)
            assertTrue(settled.members.all { it.cents == 0L } && settled.payments.isEmpty(), context)
            val reordered = ledger.shuffled(random).map { entry ->
                entry.copy(split = entry.split.entries.shuffled(random).associate { it.toPair() })
            }
            assertEquals(result, calculateBalances(reordered, currentMembers.shuffled(random)), context)
        }
    }

    @Test
    fun exhaustiveSmallBalancesAndRandomLargerOnesUseTheMinimumPaymentCount() {
        // An independent oracle maximises disjoint zero-sum groups. A group of k needs k - 1 transfers.
        for (size in 2..5) {
            fun enumerate(prefix: List<Long>) {
                if (prefix.size == size - 1) {
                    assertMinimum(prefix + -prefix.sum())
                } else {
                    for (cents in -2L..2L) enumerate(prefix + cents)
                }
            }
            enumerate(emptyList())
        }
        repeat(200) { seed ->
            val random = Random(seed)
            val prefix = List(random.nextInt(1, 8)) { random.nextLong(-20, 21) }
            assertMinimum(prefix + -prefix.sum())
        }
    }

    private fun assertMinimum(amounts: List<Long>) {
        // A temporary hub represents any zero-sum vector as a ledger; it finishes at zero and is not a member.
        val members = amounts.indices.map { "member-$it" }
        val ledger = amounts.mapIndexedNotNull { index, cents ->
            when {
                cents > 0 -> expense(index, members[index], mapOf("hub" to cents))
                cents < 0 -> expense(index, "hub", mapOf(members[index] to -cents))
                else -> null
            }
        }
        val result = calculateBalances(ledger, members)
        assertEquals(amounts, result.members.map { it.cents }, "balances=$amounts")
        assertEquals(minimumCount(amounts.filter { it != 0L }), result.payments.size, "balances=$amounts")
    }

    private fun minimumCount(amounts: List<Long>): Int {
        val sums = LongArray(1 shl amounts.size)
        val groups = IntArray(sums.size)
        for (mask in 1 until sums.size) {
            val bit = mask and -mask
            sums[mask] = sums[mask xor bit] + amounts[bit.countTrailingZeroBits()]
            var previous = 0
            for (index in amounts.indices) {
                val member = 1 shl index
                if (mask and member != 0) previous = maxOf(previous, groups[mask xor member])
            }
            groups[mask] = previous + if (sums[mask] == 0L) 1 else 0
        }
        return amounts.size - groups.last()
    }
}
