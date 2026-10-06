package com.orbit.app.data.repository

import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Empty stand-in until the Firestore repository replaces it. */
class LocalSpaceRepository : SpaceRepository {
    override fun observeSpaces(): Flow<List<Space>> = flowOf(emptyList())
}
