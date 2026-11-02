package com.orbit.app.data.mapper

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/*
 * `spaces/{spaceId}/expenses/{expenseId}`. Written with `encodeDefaults = false`, so a field whose default is
 * null is omitted, never stored as null. Required fields have no default for the same reason.
 */

/** A new expense as written. The server sets [createdAt]. */
@Serializable
internal class ExpenseDocument(
    val spaceId: String,
    val title: String,
    val paidBy: String,
    val split: Map<String, Long>,
    val date: String,
    val createdBy: String,
    val createdAt: Timestamp.ServerTimestamp,
    val kind: String,
    val itemId: String? = null,
)

/** An expense's fields as read, each null when missing or not of the expected type. */
internal data class StoredExpense(
    val title: String? = null,
    val paidBy: String? = null,
    val split: Map<String, Long>? = null,
    val date: String? = null,
    val createdBy: String? = null,
    val createdAt: Instant? = null,
    val kind: String? = null,
    val itemId: String? = null,
)
