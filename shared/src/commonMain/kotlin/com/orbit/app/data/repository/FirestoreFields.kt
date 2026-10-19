package com.orbit.app.data.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.code
import kotlin.time.Instant

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
