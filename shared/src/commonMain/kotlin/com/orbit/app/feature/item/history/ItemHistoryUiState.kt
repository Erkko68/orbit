package com.orbit.app.feature.item.history

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** What happened to an item, newest first, ending with its creation. Empty while loading, and when its read failed. */
data class ItemHistoryUiState(val entries: List<ItemHistoryEntry> = emptyList()) {
    val hasHistory: Boolean get() = entries.isNotEmpty()
}

enum class ItemHistoryAction { COMPLETED, REOPENED, CREATED }

/**
 * One thing someone did to the item, on the phone's clock. [name] is null for someone no longer in the space, and
 * [index] is the member's place in the space, which picks their colour and shape.
 */
data class ItemHistoryEntry(
    val id: String,
    val action: ItemHistoryAction,
    val name: String?,
    val isYou: Boolean,
    val initials: String,
    val photoUrl: String?,
    val index: Int,
    val date: LocalDate,
    val time: LocalTime,
)
