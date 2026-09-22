package com.orbit.app.domain.model

import kotlinx.datetime.DatePeriod

/** A recurring task whose assignee rotates through [rotation] (member ids) every [every]. */
data class Chore(
    val id: String,
    val spaceId: String,
    val title: String,
    val every: DatePeriod,
    val rotation: List<String>,
)
