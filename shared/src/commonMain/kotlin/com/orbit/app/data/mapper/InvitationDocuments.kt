package com.orbit.app.data.mapper

import com.orbit.app.domain.model.INVITATION_VALIDITY
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** `invitations/{code}` as written (ADR 0002). The server sets [createdAt]. */
@Serializable
internal class InvitationDocument(
    val spaceId: String,
    val createdBy: String,
    val createdAt: Timestamp.ServerTimestamp,
    val expiresAt: Timestamp,
    val revoked: Boolean,
)

/** The new invitation, valid from [now] for [INVITATION_VALIDITY]. */
internal fun invitationDocument(spaceId: String, createdBy: String, now: Instant) = InvitationDocument(
    spaceId = spaceId,
    createdBy = createdBy,
    createdAt = Timestamp.ServerTimestamp,
    expiresAt = (now + INVITATION_VALIDITY).let { Timestamp(it.epochSeconds, it.nanosecondsOfSecond) },
    revoked = false,
)
