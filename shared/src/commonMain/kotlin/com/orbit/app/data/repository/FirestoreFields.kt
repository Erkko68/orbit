package com.orbit.app.data.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot

/** A field of the document, or null when it is missing or not of the type asked for. */
internal inline fun <reified T> DocumentSnapshot.field(name: String): T? =
    try {
        get<T?>(name)
    } catch (_: Exception) {
        null
    }
