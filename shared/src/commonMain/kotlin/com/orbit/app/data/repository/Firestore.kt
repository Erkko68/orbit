package com.orbit.app.data.repository

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

/* The collections of ADR 0002. */
internal const val USERS = "users"
internal const val SPACES = "spaces"
internal const val MEMBERS = "members"
internal const val ITEMS = "items"
internal const val INVITATIONS = "invitations"

/**
 * A field of the document, or null when it is missing or not of the type asked for. A dotted [name]
 * reads a nested field. Read timestamps with [ServerTimestampBehavior.ESTIMATE]: one still pending
 * offline is then the local estimate instead of missing (ADR 0002).
 */
internal inline fun <reified T> DocumentSnapshot.field(
    name: String,
    serverTimestampBehavior: ServerTimestampBehavior = ServerTimestampBehavior.NONE,
): T? =
    try {
        get<T?>(name, serverTimestampBehavior)
    } catch (_: Exception) {
        null
    }

/** A timestamp field as an instant. One still pending offline is the local estimate, not missing (ADR 0002). */
internal fun DocumentSnapshot.instant(name: String): Instant? =
    field<Timestamp>(name, ServerTimestampBehavior.ESTIMATE)?.let {
        Instant.fromEpochSeconds(it.seconds, it.nanoseconds)
    }

/** The security rules refused the request. */
internal fun Throwable.isRefusedByTheRules(): Boolean =
    this is FirebaseFirestoreException && code == FirestoreExceptionCode.PERMISSION_DENIED

/** The signed-in user, for a write made as them. Throws when signed out. */
internal suspend fun AuthRepository.requireUser(): User = checkNotNull(session.first()) { "Not signed in" }
