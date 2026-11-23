package com.orbit.app.data.mapper

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/** `spaces/{spaceId}` as written (ADR 0002). The server sets [createdAt]. */
@Serializable
internal class SpaceDocument(
    val name: String,
    val accent: Int,
    val memberIds: List<String>,
    val createdBy: String,
    val ownerId: String,
    val createdAt: Timestamp.ServerTimestamp,
)

/**
 * `spaces/{spaceId}/members/{uid}` as written. Absent [photoUrl] and [invitationCode] are omitted, never stored
 * as null. [invitationCode] is only there for a member who joined with an invitation: the rules read it.
 */
@Serializable
internal class MemberDocument(
    val role: String,
    val displayName: String,
    val photoUrl: String? = null,
    val joinedAt: Timestamp.ServerTimestamp,
    val invitationCode: String? = null,
)
