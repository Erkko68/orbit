package com.orbit.app.data.mapper

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/*
 * `spaces/{spaceId}/activity/{entryId}` (#83), written in the batch of the change it records and never edited.
 * Written with `encodeDefaults = false`, so a field whose default is null is omitted, never stored as null.
 */

/**
 * One entry as written. The server sets [at]. [changes] maps each stored field path the write changed (`title`,
 * `modules.schedule`, `modules.checklist.entries.<id>.checked`) to its previous value as JSON text, `null` when it
 * was not there. A delete keeps every field it took away, so undo (#84) can put the target back.
 */
@Serializable
internal class ActivityDocument(
    val actorId: String,
    val at: Timestamp.ServerTimestamp,
    val action: String,
    val target: String,
    val targetId: String,
    val title: String,
    val changes: Map<String, String>? = null,
    /** [SOURCE_ASSISTANT] for what a confirmed assistant plan or proposal wrote. Missing for what a person did. */
    val source: String? = null,
    /** The confirmation the entry is part of, shared by every entry of one assistant plan. */
    val planId: String? = null,
)

/** An entry's fields as read, each null when missing or not of the expected type. */
internal data class StoredActivity(
    val actorId: String? = null,
    val at: Instant? = null,
    val action: String? = null,
    val target: String? = null,
    val targetId: String? = null,
    val title: String? = null,
    val changedFields: Set<String> = emptySet(),
    val source: String? = null,
    val planId: String? = null,
)

internal const val SOURCE_ASSISTANT = "assistant"
