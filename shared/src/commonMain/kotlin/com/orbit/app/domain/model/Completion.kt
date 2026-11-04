package com.orbit.app.domain.model

import kotlin.time.Instant

/**
 * One time an item was completed, or with [reopened] opened again: who and when. The history keeps every one, so
 * reopening an item keeps its completions.
 */
data class Completion(
    val id: String,
    val itemId: String,
    val completedBy: String,
    val completedAt: Instant,
    val reopened: Boolean = false,
)
