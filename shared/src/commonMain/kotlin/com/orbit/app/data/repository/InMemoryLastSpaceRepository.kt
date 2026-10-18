package com.orbit.app.data.repository

import com.orbit.app.domain.repository.LastSpaceRepository

/** Keeps the last opened space in memory, for tests and previews. */
class InMemoryLastSpaceRepository(private var spaceId: String? = null) : LastSpaceRepository {
    override fun lastSpaceId(): String? = spaceId

    override fun remember(spaceId: String) {
        this.spaceId = spaceId
    }

    override fun forget() {
        spaceId = null
    }
}
