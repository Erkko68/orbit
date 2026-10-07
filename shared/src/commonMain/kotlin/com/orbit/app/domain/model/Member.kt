package com.orbit.app.domain.model

enum class Role { OWNER, ADMIN, MEMBER }

/** One membership of a space, with a copy of the user's public profile. */
data class Member(
    val userId: String,
    val spaceId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: Role,
)
