package com.orbit.app.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.orbit.app.data.local.dao.SpaceDao
import com.orbit.app.data.local.entity.SpaceEntity

const val DATABASE_NAME = "orbit.db"

@Database(entities = [SpaceEntity::class], version = 1)
@ConstructedBy(OrbitDatabaseConstructor::class)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
}

// Room's KSP processor generates the actual implementations.
@Suppress("KotlinNoActualForExpect")
expect object OrbitDatabaseConstructor : RoomDatabaseConstructor<OrbitDatabase> {
    override fun initialize(): OrbitDatabase
}
