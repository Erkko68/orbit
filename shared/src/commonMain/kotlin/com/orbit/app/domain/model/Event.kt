package com.orbit.app.domain.model

import kotlinx.datetime.LocalDateTime

data class Event(
    val id: String,
    val spaceId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime?,
)
