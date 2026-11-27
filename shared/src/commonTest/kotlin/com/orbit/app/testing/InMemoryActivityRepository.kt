package com.orbit.app.testing

import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/**
 * Histories in memory, filled by the test with [record]: the real ones are written by the Firestore repositories in
 * the batch of each change. The first [failures] reads fail, as an unreachable server would.
 */
class InMemoryActivityRepository(private var failures: Int = 0) : ActivityRepository {
    private val entries = MutableStateFlow(emptyMap<String, List<Activity>>())

    fun record(spaceId: String, entry: Activity) {
        entries.update { it + (spaceId to it[spaceId].orEmpty() + entry) }
    }

    override fun observeActivity(spaceId: String, since: Instant): Flow<List<Activity>> = flow {
        if (failures > 0) {
            failures--
            error("The history is out of reach")
        }
        emitAll(entries.map { all -> all[spaceId].orEmpty().filter { it.at >= since }.sortedByDescending { it.at } })
    }
}
