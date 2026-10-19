package com.orbit.app.testing

import com.orbit.app.domain.repository.LastSpaceRepository

/** The last opened space in memory. */
class InMemoryLastSpaceRepository(private var spaceId: String? = null) : LastSpaceRepository {
    override fun lastSpaceId(): String? = spaceId

    override fun remember(spaceId: String) {
        this.spaceId = spaceId
    }

    override fun forget() {
        spaceId = null
    }
}
