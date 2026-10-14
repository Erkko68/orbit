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
    val createdAt: Timestamp.ServerTimestamp,
)

/** `spaces/{spaceId}/members/{uid}` as written. An absent [photoUrl] is omitted, never stored as null. */
@Serializable
internal class MemberDocument(
    val role: String,
    val displayName: String,
    val photoUrl: String? = null,
    val joinedAt: Timestamp.ServerTimestamp,
)
