package com.orbit.app.data.repository

import com.orbit.app.data.mapper.ActivityDocument
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.WriteBatch
import kotlinx.serialization.json.Json

/** Encodes the previous values an activity entry keeps (#83), as the document classes would store them. */
internal val activityJson = Json { explicitNulls = false }

/** The field at [path] of this snapshot, as an entry of an activity entry's `changes`: `null` when it is not there. */
internal inline fun <reified T> DocumentSnapshot.previous(path: String): Pair<String, String> =
    path to activityJson.encodeToString<T?>(field<T>(path))

/**
 * Appends [entry] to the history of [spaceId] in this batch, so the change and its entry are saved together or not at
 * all. The rules only let a member add their own entry, and nobody edit or delete one.
 */
internal fun WriteBatch.logActivity(firestore: FirebaseFirestore, spaceId: String, entry: ActivityDocument) {
    set(firestore.collection(SPACES).document(spaceId).collection(ACTIVITY).document, entry) { encodeDefaults = false }
}
