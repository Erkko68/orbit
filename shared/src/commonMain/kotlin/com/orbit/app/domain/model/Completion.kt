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
    /** The effort at this completion. Missing for old entries, reopenings and items without the module. */
    val effortPoints: Int? = null,
    /** False on a repeat completion of an occurrence: its original contribution stays with the first event. */
    val effortCredited: Boolean = true,
)
