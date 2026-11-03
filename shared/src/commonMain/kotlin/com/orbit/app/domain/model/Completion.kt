package com.orbit.app.domain.model

import kotlin.time.Instant

/** One time an item was completed: who and when. Reopening the item keeps it. */
data class Completion(
    val id: String,
    val itemId: String,
    val completedBy: String,
    val completedAt: Instant,
)
