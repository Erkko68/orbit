package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate

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
    /** The first and last day of the member's away period, both or neither (#48). One period at a time. */
    val awayFrom: LocalDate? = null,
    val awayUntil: LocalDate? = null,
) {
    /** The member is away on [today]: rotation skips them (#49). A period that has passed no longer counts. */
    fun isAway(today: LocalDate): Boolean = awayFrom != null && awayUntil != null && today in awayFrom..awayUntil

    /** The last day of the period while it is still ahead or running, so a finished one is not shown. */
    fun awayUntilFrom(today: LocalDate): LocalDate? = awayUntil?.takeIf { awayFrom != null && it >= today }
}

/** An away period is valid when it ends on or after the day it starts. */
fun isValidAway(from: LocalDate, until: LocalDate): Boolean = until >= from
