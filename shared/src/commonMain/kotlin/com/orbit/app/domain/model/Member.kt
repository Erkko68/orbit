package com.orbit.app.domain.model

/**
 * What a member may do in their space. firestore.rules enforces the same table (ADR 0002, "Roles").
 * Only the owner deletes the space.
 */
enum class Role {
    OWNER,
    ADMIN,
    MEMBER,
    ;

    /** Edits the space, its members and its invitations, and deletes any item. Every member edits items. */
    val managesSpace: Boolean get() = this != MEMBER

    /** Takes a member with [other] role out of the space: the owner anyone else, an admin a plain member (#264). */
    fun removes(other: Role): Boolean = when (this) {
        OWNER -> other != OWNER
        ADMIN -> other == MEMBER
        MEMBER -> false
    }
}

/** One membership of a space, with a copy of the user's public profile. */
data class Member(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val role: Role,
)
