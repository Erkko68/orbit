package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import dev.gitlive.firebase.firestore.Timestamp

/**
 * A space from the fields of its document, or null when it has no name: a malformed document is
 * skipped by the caller instead of crashing the list.
 */
internal fun mapSpace(id: String, name: String?, accent: Int?, memberIds: List<String>?): Space? =
    name?.let { Space(id = id, name = it, accent = accent ?: 0, memberIds = memberIds.orEmpty()) }

/** A member from the fields of its document. The document id is the user's uid. */
internal fun mapMember(
    userId: String,
    displayName: String?,
    photoUrl: String?,
    role: String?,
) = Member(
    userId = userId,
    displayName = displayName.orEmpty(),
    photoUrl = photoUrl,
    role = roleOf(role),
)

/** The role as stored in a member document (ADR 0002). */
internal fun roleValue(role: Role): String = when (role) {
    Role.OWNER -> "owner"
    Role.ADMIN -> "admin"
    Role.MEMBER -> "member"
}

/** An unknown role, such as one written by a newer build, gets the least privilege. */
private fun roleOf(role: String?): Role = when (role) {
    "owner" -> Role.OWNER
    "admin" -> Role.ADMIN
    else -> Role.MEMBER
}

/** The new space: its only member is its creator. */
internal fun spaceDocument(name: String, accent: Int, creatorId: String) = SpaceDocument(
    name = name,
    accent = accent,
    memberIds = listOf(creatorId),
    createdBy = creatorId,
    createdAt = Timestamp.ServerTimestamp,
)

/** The creator's membership of the space they just made. */
internal fun ownerMemberDocument(displayName: String, photoUrl: String?) = MemberDocument(
    role = "owner",
    displayName = displayName,
    photoUrl = photoUrl,
    joinedAt = Timestamp.ServerTimestamp,
)
