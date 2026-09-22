package com.orbit.app.data.mapper

import com.orbit.app.data.local.entity.SpaceEntity
import com.orbit.app.domain.model.Space

fun SpaceEntity.toDomain() = Space(id = id, name = name)

fun Space.toEntity() = SpaceEntity(id = id, name = name)
