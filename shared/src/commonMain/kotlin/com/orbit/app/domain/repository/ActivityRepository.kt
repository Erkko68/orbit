package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Activity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * The history of a space (#83). Read only: the item, expense and plan repositories append to it in the batch of each
 * write, so an entry never lands without its change nor a change without its entry. Throws on failure.
 */
fun interface ActivityRepository {
    /** What changed in the space since [since], newest first. */
    fun observeActivity(spaceId: String, since: Instant): Flow<List<Activity>>
}
