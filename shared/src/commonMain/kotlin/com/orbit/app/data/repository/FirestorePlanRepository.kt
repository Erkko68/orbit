package com.orbit.app.data.repository

import com.orbit.app.data.mapper.itemDocument
import com.orbit.app.data.mapper.ownerMemberDocument
import com.orbit.app.data.mapper.spaceDocument
import com.orbit.app.domain.model.PlanReview
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.PlanRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * One batch: the new space with the user as owner, as [FirestoreSpaceRepository.createSpace] writes it, every accepted
 * item, and what the assistant now knows about the space. The rules check an item against the space as the batch
 * leaves it, so a new space's items pass.
 */
class FirestorePlanRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
) : PlanRepository {
    override suspend fun confirm(spaceId: String, plan: PlanReview): String {
        val user = auth.requireUser()
        val batch = firestore.batch()
        val space = if (spaceId.isNotEmpty()) {
            firestore.collection(SPACES).document(spaceId).also { space ->
                plan.summary?.let { summary -> batch.updateFields(space) { "assistantSummary" to summary } }
            }
        } else {
            val proposed = checkNotNull(plan.space) { "A plan for a new space proposes one" }
            firestore.collection(SPACES).document.also {
                batch.set(it, spaceDocument(proposed.name, proposed.accent, user.id, plan.summary.orEmpty()))
                batch.set(it.collection(MEMBERS).document(user.id), ownerMemberDocument(user.displayName, user.photoUrl))
            }
        }
        plan.accepted.forEach { proposal ->
            batch.set(space.collection(ITEMS).document, itemDocument(space.id, proposal.item, user.id)) {
                encodeDefaults = false
            }
        }
        writeLocally("Plan for space ${space.id}") { batch.commit() }
        return space.id
    }
}
