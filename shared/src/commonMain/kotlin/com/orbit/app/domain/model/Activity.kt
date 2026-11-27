package com.orbit.app.domain.model

import kotlin.time.Instant

/** What a write did to its target. */
enum class ActivityAction { CREATE, EDIT, COMPLETE, REOPEN, DELETE }

/** What kind of thing a write changed. */
enum class ActivityTarget { ITEM, EXPENSE }

/**
 * One entry of a space's history (#83): [actorId] did [action] to an item or an expense at [at]. [title] is the
 * target's title as the write left it, so a deleted one still reads. [changedFields] are the stored fields an edit or a
 * delete changed, whose previous values the entry keeps for undo (#84). [byAssistant] marks what a confirmed assistant
 * plan or proposal wrote, and [planId] ties together the entries of one confirmation.
 */
data class Activity(
    val id: String,
    val actorId: String,
    val at: Instant,
    val action: ActivityAction,
    val target: ActivityTarget,
    val targetId: String,
    val title: String,
    val changedFields: Set<String> = emptySet(),
    val byAssistant: Boolean = false,
    val planId: String? = null,
)
