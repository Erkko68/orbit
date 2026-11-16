package com.orbit.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute

@Serializable
data object CreateAccountRoute

@Serializable
data object SpacesRoute

@Serializable
data object NewSpaceRoute

@Serializable
data object AccountRoute

/** One space, with its bottom bar: calendar, tasks and expenses (ADR 0005). */
@Serializable
data class SpaceHomeRoute(val spaceId: String)

/** The settings of a space: its name and colour, its members, invitations, and leaving or deleting it. */
@Serializable
data class SpaceSettingsRoute(val spaceId: String)

/** One member of a space: their role, what they have and what they did lately. */
@Serializable
data class MemberRoute(val spaceId: String, val userId: String)

/**
 * The assistant's interview, over the bar. #44 opens it from a space, [spaceId], and #77 without one, to set up a new
 * space.
 */
@Serializable
data class InterviewRoute(val spaceId: String = "")

/** The review board of the plan the interview proposed (#75), for [spaceId] or, when it is empty, a new space. */
@Serializable
data class PlanReviewRoute(val spaceId: String)

/**
 * One item of a space. Without [itemId] it is a new one, not written until the user creates it. With [proposalId] it
 * is a proposal of the plan under review, which Done hands back to the board (#75); its space may not exist yet.
 */
@Serializable
data class ItemRoute(val spaceId: String, val itemId: String = "", val proposalId: String = "")

/**
 * An expense form, above the space bar. An empty id creates an entry only on Save. From an item's Cost row the same
 * form plans its cost instead (#300): the cost of [itemId], or with a [draftCost] (see [encodePlannedCost]) the cost of
 * an item not created yet, which Save hands back instead of writing. [sourceItemId] opens a new ledger expense
 * from a checklist; it never plans a cost.
 */
@Serializable
data class ExpenseRoute(
    val spaceId: String,
    val expenseId: String = "",
    val itemId: String = "",
    val draftCost: String = "",
    /** Opens a new ledger expense from a checklist, independently of cost planning. */
    val sourceItemId: String = "",
)
