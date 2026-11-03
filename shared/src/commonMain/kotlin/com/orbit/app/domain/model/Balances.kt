package com.orbit.app.domain.model

/** A member's net amount in Long cents: positive means they receive, negative means they pay. */
data class MemberBalance(val memberId: String, val cents: Long)

/** A proposed transfer, not a recorded settlement. Only the user's confirmation may save one (#57). */
data class SuggestedPayment(val paidBy: String, val paidTo: String, val cents: Long)

/** Derived from the ledger, never stored. Members are ordered by uid; payments settle all their balances. */
data class Balances(val members: List<MemberBalance>, val payments: List<SuggestedPayment>)
