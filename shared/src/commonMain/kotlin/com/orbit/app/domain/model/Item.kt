package com.orbit.app.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

sealed interface ItemStatus {
    data object Open : ItemStatus
    data class Done(val by: String, val at: Instant) : ItemStatus
}

/** An item as read from Firestore. */
data class Item(
    val id: String,
    val title: String,
    val description: String?,
    val status: ItemStatus,
    val createdBy: String,
    val createdAt: Instant,
    val modules: ItemModules = ItemModules(),
    /**
     * The expense this occurrence's cost added. Reopening keeps it, so completing again adds none; rolling forward
     * clears it (ADR 0014).
     */
    val costExpenseId: String? = null,
)

/** The item is still to do, it is [userId]'s, and it is due by [today]. */
fun Item.isDueFor(userId: String, today: LocalDate): Boolean {
    val deadline = modules.schedule?.deadline ?: return false
    return status is ItemStatus.Open && modules.isAssignedTo(userId) && deadline <= today
}

/** What a person or the assistant fills in. The repository adds id, creator and timestamps. */
data class NewItem(
    val title: String,
    val description: String? = null,
    val modules: ItemModules = ItemModules(),
)
