package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Balances
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.MemberBalance
import com.orbit.app.domain.model.SuggestedPayment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class CalculateBalancesTest {
    private fun expense(payer: String, vararg shares: Pair<String, Long>, kind: ExpenseKind = ExpenseKind.EXPENSE) =
        Expense(
            "expense", "Groceries", payer, shares.toMap(), LocalDate(2026, 11, 3), payer,
            Instant.fromEpochSeconds(0), kind
        )

    @Test
    fun anEmptyLedgerKeepsCurrentMembersAtZeroOnceAndInUidOrder() {
        assertEquals(
            Balances(listOf(MemberBalance("ana", 0), MemberBalance("ben", 0)), emptyList()),
            calculateBalances(emptyList(), listOf("ben", "ana", "ana")),
        )
        assertEquals(Balances(emptyList(), emptyList()), calculateBalances(emptyList(), emptyList()))
    }

    @Test
    fun thePayerReceivesTheStoredSharesMinusTheirOwnShare() {
        val result = calculateBalances(
            listOf(expense("ana", "ana" to 1000, "ben" to 1000, "cam" to 1000)),
            listOf("ana", "ben", "cam")
        )
        assertEquals(
            listOf(MemberBalance("ana", 2000), MemberBalance("ben", -1000), MemberBalance("cam", -1000)),
            result.members
        )
        assertEquals(
            listOf(SuggestedPayment("ben", "ana", 1000), SuggestedPayment("cam", "ana", 1000)),
            result.payments
        )
    }

    @Test
    fun thePayerNeedNotOweAShareAndStoredRoundingIsPreserved() {
        val result = calculateBalances(
            listOf(expense("ana", "ben" to 334, "cam" to 333, "dan" to 333)),
            listOf("ana", "ben", "cam", "dan")
        )
        assertEquals(
            listOf(
                MemberBalance("ana", 1000), MemberBalance("ben", -334),
                MemberBalance("cam", -333), MemberBalance("dan", -333)
            ), result.members
        )
    }

    @Test
    fun aPayersOwnExpenseDoesNotCreateADebt() {
        val result = calculateBalances(listOf(expense("ana", "ana" to 500)), listOf("ana"))
        assertEquals(Balances(listOf(MemberBalance("ana", 0)), emptyList()), result)
    }

    @Test
    fun partialAndFullSettlementsMoveMoneyInTheSameDirectionAsExpenses() {
        val purchase = expense("ana", "ben" to 1200)
        val partial = expense("ben", "ana" to 400, kind = ExpenseKind.SETTLEMENT)
        val rest = expense("ben", "ana" to 800, kind = ExpenseKind.SETTLEMENT)
        val members = listOf("ana", "ben")
        assertEquals(
            listOf(SuggestedPayment("ben", "ana", 800)),
            calculateBalances(listOf(purchase, partial), members).payments
        )
        assertEquals(
            Balances(listOf(MemberBalance("ana", 0), MemberBalance("ben", 0)), emptyList()),
            calculateBalances(listOf(purchase, partial, rest), members)
        )
        // Removing the settlement restores the original debt; the use case stores no previous result.
        assertEquals(
            listOf(SuggestedPayment("ben", "ana", 1200)),
            calculateBalances(listOf(purchase), members).payments
        )
    }

    @Test
    fun formerDebtorsAndCreditorsRemainUntilTheirBalanceIsZero() {
        val purchase = expense("former-payer", "former-debtor" to 100, "ana" to 200)
        val first = expense("former-debtor", "former-payer" to 100, kind = ExpenseKind.SETTLEMENT)
        val last = expense("ana", "former-payer" to 200, kind = ExpenseKind.SETTLEMENT)
        assertEquals(
            listOf(
                MemberBalance("ana", -200), MemberBalance("former-debtor", -100),
                MemberBalance("former-payer", 300)
            ), calculateBalances(listOf(purchase), listOf("ana")).members
        )
        assertEquals(
            listOf(MemberBalance("ana", -200), MemberBalance("former-payer", 200)),
            calculateBalances(listOf(purchase, first), listOf("ana")).members
        )
        assertEquals(
            listOf(MemberBalance("ana", 0)),
            calculateBalances(listOf(purchase, first, last), listOf("ana")).members
        )
        assertEquals(3, calculateBalances(listOf(purchase), emptyList()).members.size)
    }

    @Test
    fun aReturningCurrentMemberRemainsEvenAfterSettling() {
        val ledger = listOf(expense("ana", "ben" to 100), expense("ben", "ana" to 100))
        assertEquals(
            listOf(MemberBalance("ana", 0), MemberBalance("ben", 0)),
            calculateBalances(ledger, listOf("ana", "ben")).members
        )
    }

    @Test
    fun itFindsFewerPaymentsThanLargestDebtToLargestCreditGreedy() {
        // Credits 8, 7, 5 and debts 10, 8, 2. Largest-first takes five payments; four suffice.
        val ledger = listOf(
            expense("a", "e" to 8), expense("b", "d" to 7),
            expense("c", "d" to 3, "f" to 2)
        )
        val result = calculateBalances(ledger, listOf("a", "b", "c", "d", "e", "f"))
        assertEquals(4, result.payments.size)
    }

    @Test
    fun reversingTheLedgerMembersAndShareOrderDoesNotChangeTheResult() {
        val ledger = listOf(expense("ana", "ben" to 123, "cam" to 456), expense("cam", "ana" to 78))
        val members = listOf("ana", "ben", "cam")
        val reversed = ledger.reversed().map { it.copy(split = it.split.entries.reversed().associate { it.toPair() }) }
        assertEquals(calculateBalances(ledger, members), calculateBalances(reversed, members.reversed()))
    }

    @Test
    fun amountsAboveIntRangeAndOneCentDebtsRemainExact() {
        val amount = Int.MAX_VALUE.toLong() + 1
        assertEquals(
            listOf(SuggestedPayment("ben", "ana", amount), SuggestedPayment("cam", "ana", 1)),
            calculateBalances(listOf(expense("ana", "ben" to amount, "cam" to 1)), listOf("ana", "ben", "cam")).payments
        )
    }

    @Test
    fun cancellingOwnSharesDoNotOverflowTheGrossExpenseTotal() {
        val ledger = listOf(expense("ana", "ana" to Long.MAX_VALUE, "ben" to 1))
        assertEquals(
            listOf(SuggestedPayment("ben", "ana", 1)),
            calculateBalances(ledger, listOf("ana", "ben")).payments
        )
    }

    @Test
    fun theSmallestLongDebtCanBeSettledWithoutNegatingIt() {
        val ledger = listOf(expense("ana", "cam" to Long.MAX_VALUE), expense("ben", "cam" to 1))
        val result = calculateBalances(ledger, listOf("ana", "ben", "cam"))
        assertEquals(Long.MIN_VALUE, result.members.single { it.memberId == "cam" }.cents)
        assertEquals(
            listOf(SuggestedPayment("cam", "ana", Long.MAX_VALUE), SuggestedPayment("cam", "ben", 1)),
            result.payments
        )
    }

    @Test
    fun balanceOverflowFailsInsteadOfSilentlyWrappingMoney() {
        assertFailsWith<IllegalArgumentException> {
            calculateBalances(
                listOf(expense("ana", "ben" to Long.MAX_VALUE), expense("ana", "cam" to 1)),
                listOf("ana", "ben", "cam")
            )
        }
        assertFailsWith<IllegalArgumentException> {
            calculateBalances(
                listOf(expense("ana", "cam" to Long.MAX_VALUE), expense("ben", "cam" to 2)),
                listOf("ana", "ben", "cam")
            )
        }
    }
}
