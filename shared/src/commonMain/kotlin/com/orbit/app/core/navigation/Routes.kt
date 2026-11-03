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

/** One space, with its bottom bar: home, tasks and expenses (ADR 0005). */
@Serializable
data class SpaceHomeRoute(val spaceId: String)

/** The assistant's interview, over the bar. #44 opens it from a space and #77 from a new one. */
@Serializable
data object InterviewRoute

/** One item of a space. Without [itemId] it is a new one, not written until it has a name. */
@Serializable
data class ItemRoute(val spaceId: String, val itemId: String = "")
