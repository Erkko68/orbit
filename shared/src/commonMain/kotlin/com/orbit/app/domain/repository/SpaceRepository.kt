package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Space
import kotlinx.coroutines.flow.Flow

interface SpaceRepository {
    fun observeSpaces(): Flow<List<Space>>
}
