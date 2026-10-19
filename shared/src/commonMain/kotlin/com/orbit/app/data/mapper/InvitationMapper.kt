package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Invitation
import kotlin.time.Instant

/** An invitation from the fields of its document, or null when one it cannot do without is missing. */
internal fun mapInvitation(
    code: String,
    spaceId: String?,
    createdBy: String?,
    createdAt: Instant?,
    expiresAt: Instant?,
    revoked: Boolean?,
): Invitation? {
    if (spaceId == null || createdBy == null || createdAt == null || expiresAt == null) return null
    return Invitation(code, spaceId, createdBy, createdAt, expiresAt, revoked ?: false)
}
