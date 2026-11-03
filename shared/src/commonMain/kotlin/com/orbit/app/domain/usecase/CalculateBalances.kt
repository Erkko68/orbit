package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Balances
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.MemberBalance
import com.orbit.app.domain.model.SuggestedPayment

/**
 * Paid minus owed, including settlements (ADR 0003). [memberIds] are the current members: they remain even at
 * zero; anyone else in [ledger] remains only while owed or owing money. Names come from the caller's members
 * and Space.formerMembers. Stored shares are already rounded; this never splits or rounds an amount again.
 *
 * Returns the fewest direct debtor-to-creditor payments, with uid order breaking ties deterministically.
 * The exact search is exponential in the number of non-zero balances, intended for small shared spaces.
 * Throws [IllegalArgumentException] if accumulating a balance exceeds Long cents instead of wrapping money.
 */
fun calculateBalances(ledger: List<Expense>, memberIds: Collection<String>): Balances {
    val currentMembers = memberIds.toSet()
    val amounts = currentMembers.associateWith { 0L }.toMutableMap()
    for (expense in ledger) {
        for ((memberId, cents) in expense.split) {
            // The payer's own share cancels out, without adding a possibly overflowing gross total.
            if (memberId == expense.paidBy) continue
            amounts[expense.paidBy] = (amounts[expense.paidBy] ?: 0L).plusCents(cents)
            amounts[memberId] = (amounts[memberId] ?: 0L).plusCents(-cents)
        }
    }
    val members = amounts.keys.sorted()
        .map { MemberBalance(it, amounts.getValue(it)) }
        .filter { it.cents != 0L || it.memberId in currentMembers }
    val outstanding = members.filter { it.cents != 0L }
    return Balances(members, PaymentSearch(outstanding).solve(outstanding.map { it.cents }))
}

private fun Long.plusCents(other: Long): Long {
    require(if (other > 0) this <= Long.MAX_VALUE - other else this >= Long.MIN_VALUE - other) {
        "Balance exceeds Long cents"
    }
    return this + other
}

/**
 * A minimum transfer graph can be a forest: any cycle can lose an edge without changing the balances.
 * A leaf's payment closes that member, so trying every debtor/creditor pair and closing at least one endpoint
 * includes an optimal plan. Every step removes a non-zero balance, hence at most n - 1 payments.
 */
private class PaymentSearch(private val members: List<MemberBalance>) {
    private val solutions = mutableMapOf<List<Long>, List<SuggestedPayment>>()

    fun solve(amounts: List<Long>): List<SuggestedPayment> = solutions.getOrPut(amounts) { search(amounts) }

    private fun search(amounts: List<Long>): List<SuggestedPayment> {
        val debtors = amounts.indices.filter { amounts[it] < 0 }
        val creditors = amounts.indices.filter { amounts[it] > 0 }
        if (debtors.isEmpty()) return emptyList()
        val lowerBound = maxOf(debtors.size, creditors.size)
        var best: List<SuggestedPayment>? = null
        // Equal amounts are interchangeable for the minimum count; keep the first uid of each.
        for (debtor in debtors.distinctBy { amounts[it] }) {
            for (creditor in creditors.distinctBy { amounts[it] }) {
                val debt = amounts[debtor]
                val credit = amounts[creditor]
                // Comparing before negating also handles Long.MIN_VALUE debts.
                val cents = if (debt < -credit) credit else -debt
                val next = amounts.toMutableList()
                next[debtor] = debt + cents
                next[creditor] = credit - cents
                val payment = SuggestedPayment(members[debtor].memberId, members[creditor].memberId, cents)
                val candidate = listOf(payment) + solve(next.toList())
                if (best == null || candidate.size < best.size) best = candidate
                // Every debtor must pay and every creditor must receive at least once.
                if (candidate.size == lowerBound) return candidate
            }
        }
        return checkNotNull(best)
    }
}
