package com.orbit.app.core.navigation

import com.orbit.app.domain.model.Cost
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/**
 * The key a new item's cost comes back under, in the item screen's `SavedStateHandle`. An item not created yet plans its
 * cost on the expense screen too (#300): the cost goes there and back as text, and is written with the item on Create.
 */
internal const val PLANNED_COST = "plannedCost"

@Serializable
private class PlannedCost(
    val paidBy: String,
    val split: Map<String, Long>,
    val customAllocation: Map<String, Boolean>? = null,
)

/** "null" when there is no cost, so it is never empty: an empty text means nothing was planned. */
fun encodePlannedCost(cost: Cost?): String = Json.encodeToString(
    PlannedCost.serializer().nullable,
    cost?.let { PlannedCost(it.paidBy, it.split, it.customAllocation) },
)

fun decodePlannedCost(text: String): Cost? =
    Json.decodeFromString(PlannedCost.serializer().nullable, text)?.let { Cost(it.paidBy, it.split, it.customAllocation) }
