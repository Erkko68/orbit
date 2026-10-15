package com.orbit.app.data.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.ServerTimestampBehavior

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
