package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.StoredActivity
import com.orbit.app.data.mapper.mapActivity
import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.repository.ActivityRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * The history of a space in Firestore (#83), through a live listener that the offline cache answers first. A range and
 * an order on the same field need no composite index.
 */
class FirestoreActivityRepository(private val firestore: FirebaseFirestore) : ActivityRepository {
    /** Refused while a space made a moment ago is not on the server yet, so asked again for a few seconds. */
    override fun observeActivity(spaceId: String, since: Instant): Flow<List<Activity>> =
        firestore.collection(SPACES).document(spaceId).collection(ACTIVITY)
            .where { AT greaterThanOrEqualTo Timestamp(since.epochSeconds, since.nanosecondsOfSecond) }
            .orderBy(AT, Direction.DESCENDING)
            .snapshots.map { snapshot -> snapshot.documents.mapNotNull(::toActivity) }
            .flowOn(Dispatchers.Default)
            .retryWhileRefused()

    private fun toActivity(document: DocumentSnapshot): Activity? {
        val stored = StoredActivity(
            actorId = document.field("actorId"),
            at = document.instant(AT),
            action = document.field("action"),
            target = document.field("target"),
            targetId = document.field("targetId"),
            title = document.field("title"),
            changedFields = document.field<Map<String, String>>("changes")?.keys.orEmpty(),
            source = document.field("source"),
            planId = document.field("planId"),
        )
        return mapActivity(document.id, stored).also {
            if (it == null) Logger.w { "Skipping the malformed activity entry ${document.id}" }
        }
    }

    private companion object {
        const val AT = "at"
    }
}
