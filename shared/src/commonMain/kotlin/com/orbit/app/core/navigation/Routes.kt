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

/** The assistant's interview, over the bar. #44 opens it from a space and #77 from a new one. */
@Serializable
data object InterviewRoute

/** One item of a space. Without [itemId] it is a new one, not written until the user creates it. */
@Serializable
data class ItemRoute(val spaceId: String, val itemId: String = "")

/** An expense form, above the space bar. An empty id creates an entry only on Save. */
@Serializable
data class ExpenseRoute(val spaceId: String, val expenseId: String = "")
