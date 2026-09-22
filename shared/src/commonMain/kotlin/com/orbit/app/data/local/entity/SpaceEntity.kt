package com.orbit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
)
