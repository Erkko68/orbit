package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate

data class Task(
    val id: String,
    val spaceId: String,
    val title: String,
    val assigneeId: String?,
    val due: LocalDate?,
    val done: Boolean,
)
