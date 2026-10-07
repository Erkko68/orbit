package com.orbit.app.domain.model

import kotlin.time.Instant

enum class Preset { TASK, SHOPPING_LIST, EVENT }

sealed interface ItemStatus {
    data object Open : ItemStatus
    data class Done(val by: String, val at: Instant) : ItemStatus
}

/** An item as read from Firestore. */
data class Item(
    val id: String,
    val spaceId: String,
    val title: String,
    val description: String?,
    val status: ItemStatus,
    val preset: Preset?,
    val createdBy: String,
    val createdAt: Instant,
    val modules: ItemModules = ItemModules(),
)

/** What a person, a preset or the assistant fills in. The repository adds id, creator and timestamps. */
data class NewItem(
    val title: String,
    val description: String? = null,
    val preset: Preset? = null,
    val modules: ItemModules = ItemModules(),
)
