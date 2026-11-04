package com.orbit.app.data.mapper

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/*
 * `spaces/{spaceId}/items/{itemId}` (ADR 0002). Written with `encodeDefaults = false`, so a field whose
 * default is null is omitted, never stored as null. Required fields have no default for the same reason.
 */

/** A new item as written. The server sets [createdAt]. */
@Serializable
internal class ItemDocument(
    val spaceId: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val createdBy: String,
    val createdAt: Timestamp.ServerTimestamp,
    val modules: ModulesDocument,
)

/** The `modules` map: a key per module present. */
@Serializable
internal class ModulesDocument(
    val schedule: ScheduleDocument? = null,
    val recurrence: RecurrenceDocument? = null,
    val assignment: AssignmentDocument? = null,
    val checklist: ChecklistDocument? = null,
    val reminder: ReminderDocument? = null,
)

@Serializable
internal class ScheduleDocument(
    val date: String,
    val time: String? = null,
    val endDate: String? = null,
    val durationMinutes: Int? = null,
)

/** [unit] is `day`, `week` or `month`; [weekdays] are ISO day numbers (Monday is 1), only for `week`. */
@Serializable
internal class RecurrenceDocument(
    val unit: String,
    val every: Int,
    val weekdays: List<Int>? = null,
    val dayOfMonth: Int? = null,
    val until: String? = null,
)

/** [assigneeId] is how one member was stored before an item could have several: read, never written. */
@Serializable
internal class AssignmentDocument(
    val mode: String,
    val assigneeIds: List<String>? = null,
    val assigneeId: String? = null,
)

@Serializable
internal class ChecklistDocument(val entries: Map<String, EntryDocument>)

/**
 * Nullable without defaults: always written, but read leniently. Ticking an entry another member removed
 * offline leaves `{checked}` alone under its id, which is skipped instead of breaking the whole list.
 */
@Serializable
internal class EntryDocument(
    val text: String?,
    val checked: Boolean?,
    val quantity: Int? = null,
    val order: Int?,
)

@Serializable
internal class ReminderDocument(val offsetsMinutes: List<Int>)

/** An item's fields as read, each null when missing or not of the expected type. */
internal data class StoredItem(
    val title: String? = null,
    val description: String? = null,
    /** The long text of ADR 0008, retired by ADR 0010: it reads as the end of [description]. */
    val details: String? = null,
    val status: String? = null,
    val createdBy: String? = null,
    val createdAt: Instant? = null,
    val completedBy: String? = null,
    val completedAt: Instant? = null,
    val moduleKeys: Set<String> = emptySet(),
    val modules: ModulesDocument = ModulesDocument(),
)

/** `spaces/{spaceId}/completions/{completionId}`, written with the item it completes. The server sets [completedAt]. */
@Serializable
internal class CompletionDocument(
    val itemId: String,
    val completedBy: String,
    val completedAt: Timestamp.ServerTimestamp,
    /** [ACTION_REOPEN] for a reopening. Missing on a completion, which is what every entry was before it. */
    val action: String? = null,
)

internal const val ACTION_REOPEN = "reopen"
