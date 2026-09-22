package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate

/** Amounts are in minor units (cents) to avoid floating-point money. */
data class Expense(
    val id: String,
    val spaceId: String,
    val title: String,
    val amountCents: Long,
    val paidById: String,
    val splitAmongIds: List<String>,
    val date: LocalDate,
)
