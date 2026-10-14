package com.orbit.app.domain.model

import kotlinx.datetime.TimeZone

/**
 * The signed-in user's private profile. [language] and [timeZone] come from the phone, not from a
 * setting. [email] is the sign-in account's, not part of the profile document.
 */
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val language: String,
    val timeZone: TimeZone,
    val email: String? = null,
)
