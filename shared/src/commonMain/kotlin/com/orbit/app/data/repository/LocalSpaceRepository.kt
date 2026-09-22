package com.orbit.app.data.repository

import com.orbit.app.data.local.dao.SpaceDao
import com.orbit.app.data.mapper.toDomain
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalSpaceRepository(private val dao: SpaceDao) : SpaceRepository {
    override fun observeSpaces(): Flow<List<Space>> =
        dao.observeAll().map { spaces -> spaces.map { it.toDomain() } }
}
