package com.orbit.app.domain.repository

import com.orbit.app.domain.model.PlanReview

/** Saves what the user confirmed of the assistant's plan (#76). Acts as the signed-in user. */
fun interface PlanRepository {
    /**
     * Writes the [plan]'s accepted proposals as items of [spaceId] or, when it is empty, of the space the plan proposes,
     * created owned by the user, all in one batch: everything is saved or nothing is. Returns the space's id. Like any
     * write it returns once applied locally, so it works offline. Throws on failure.
     */
    suspend fun confirm(spaceId: String, plan: PlanReview): String
}
