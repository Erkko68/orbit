package com.orbit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.orbit.app.data.local.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY name")
    fun observeAll(): Flow<List<SpaceEntity>>
}
