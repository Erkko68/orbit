package com.orbit.app.domain.model

import kotlin.time.Instant

/**
 * An invitation to a space: whoever has the [code] can join it until [expiresAt], or until it is [revoked]
 * (ADR 0002).
 */
data class Invitation(
    val code: String,
    val spaceId: String,
    val createdBy: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revoked: Boolean,
) {
    /** The link that opens this invitation in the app. */
    val link: String get() = invitationLink(code)

    /** Neither revoked nor past its expiry at [now]. */
    fun isActive(now: Instant): Boolean = !revoked && now < expiresAt
}
