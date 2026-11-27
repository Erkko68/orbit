package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import dev.gitlive.firebase.firestore.Timestamp

/** How [action] is stored: its name in lower case (`create`, `edit`, `complete`, `reopen`, `delete`). */
internal fun actionKey(action: ActivityAction): String = action.name.lowercase()

/** How [target] is stored: `item` or `expense`. */
internal fun targetKey(target: ActivityTarget): String = target.name.lowercase()

/** A new entry, by [actorId], as written in the batch of its change. */
internal fun activityDocument(
    actorId: String,
    action: ActivityAction,
    target: ActivityTarget,
    targetId: String,
    title: String,
    changes: Map<String, String> = emptyMap(),
    planId: String? = null,
) = ActivityDocument(
    actorId = actorId,
    at = Timestamp.ServerTimestamp,
    action = actionKey(action),
    target = targetKey(target),
    targetId = targetId,
    title = title,
    changes = changes.ifEmpty { null },
    source = planId?.let { SOURCE_ASSISTANT },
    planId = planId,
)

/**
 * An entry from its stored fields, or null when a required one is missing or its action or target is one this build
 * does not know: a malformed or newer entry is skipped by the caller rather than misread.
 */
internal fun mapActivity(id: String, stored: StoredActivity): Activity? {
    val actorId = stored.actorId ?: return null
    val at = stored.at ?: return null
    val action = ActivityAction.entries.firstOrNull { actionKey(it) == stored.action } ?: return null
    val target = ActivityTarget.entries.firstOrNull { targetKey(it) == stored.target } ?: return null
    val targetId = stored.targetId ?: return null
    return Activity(
        id = id,
        actorId = actorId,
        at = at,
        action = action,
        target = target,
        targetId = targetId,
        title = stored.title.orEmpty(),
        changedFields = stored.changedFields,
        byAssistant = stored.source == SOURCE_ASSISTANT,
        planId = stored.planId,
    )
}
