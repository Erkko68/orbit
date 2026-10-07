package com.orbit.app.domain.model

import kotlinx.datetime.TimeZone

/** The signed-in user's private profile. */
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val language: String,
    val timeZone: TimeZone,
)
