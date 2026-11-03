package com.orbit.app.feature.item.history

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** The latest times an item was completed, newest first. Empty while loading, and when its read failed. */
data class ItemHistoryUiState(val completions: List<ItemCompletion> = emptyList()) {
    val hasHistory: Boolean get() = completions.isNotEmpty()
}

/**
 * One completion, on the phone's clock. [name] is null for someone no longer in the space, and [index] is the member's
 * place in the space, which picks their colour and shape.
 */
data class ItemCompletion(
    val id: String,
    val name: String?,
    val isYou: Boolean,
    val initials: String,
    val photoUrl: String?,
    val index: Int,
    val date: LocalDate,
    val time: LocalTime,
)
