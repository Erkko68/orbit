package com.orbit.app.data.mapper

import kotlinx.serialization.Serializable

/*
 * What the assistant proposes, as the JSON Schemas in `server/schemas/` define it (#64). A field here changes with its
 * schema, and the samples in `server/schemas/samples/` parse on both sides. Read from the server's reply, never
 * written to Firestore as is: the user confirms each proposal first (#76).
 */

/** The version of `plan.v<n>.json` this app reads. */
internal const val PLAN_VERSION = 1

/** `plan.v1.json`: the new space when the interview was for one, and the items to add. */
@Serializable
internal data class PlanDocument(
    val version: Int,
    val space: SpaceProposalDocument? = null,
    val items: List<ItemProposalDocument>,
)

/** `space.v1.json`. [accent] is an index into the space colours, as a space stores it. */
@Serializable
internal data class SpaceProposalDocument(val name: String, val accent: Int)

/** `item.v1.json`. */
@Serializable
internal data class ItemProposalDocument(
    val title: String,
    val description: String? = null,
    val modules: ProposedModulesDocument,
)

@Serializable
internal data class ProposedModulesDocument(
    val schedule: ProposedScheduleDocument? = null,
    val assignment: ProposedAssignmentDocument? = null,
    val checklist: ProposedChecklistDocument? = null,
    val reminder: ProposedReminderDocument? = null,
)

@Serializable
internal data class ProposedScheduleDocument(
    val date: String,
    val time: String? = null,
    val endDate: String? = null,
    val durationMinutes: Int? = null,
)

/** `member` with the [assignees] by their handle in the request (`m1`), never a uid, or `anyone` without them. */
@Serializable
internal data class ProposedAssignmentDocument(val mode: String, val assignees: List<String> = emptyList())

/** The entries in order. The app gives them their ids when it saves them. */
@Serializable
internal data class ProposedChecklistDocument(val entries: List<ProposedEntryDocument>)

@Serializable
internal data class ProposedEntryDocument(val text: String, val quantity: Int? = null)

@Serializable
internal data class ProposedReminderDocument(val offsetsMinutes: List<Int>)
