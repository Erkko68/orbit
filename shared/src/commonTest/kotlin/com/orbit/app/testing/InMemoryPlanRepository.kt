package com.orbit.app.testing

import com.orbit.app.domain.model.PlanReview
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.PlanRepository
import com.orbit.app.domain.repository.SpaceRepository

/** Saves a plan into the in-memory [spaces] and [items], one write after another: a failure here is not atomic. */
internal class InMemoryPlanRepository(
    private val spaces: SpaceRepository,
    private val items: ItemRepository,
) : PlanRepository {
    override suspend fun confirm(spaceId: String, plan: PlanReview): String {
        val id = spaceId.ifEmpty { checkNotNull(plan.space).let { spaces.createSpace(it.name, it.accent) } }
        plan.accepted.forEach { items.create(id, it.item) }
        return id
    }
}
