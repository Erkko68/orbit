package com.orbit.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute

@Serializable
data object CreateAccountRoute

@Serializable
data object SpacesRoute

@Serializable
data object AccountRoute

/** One space, with its bottom bar: home, tasks and expenses (ADR 0005). */
@Serializable
data class SpaceHomeRoute(val spaceId: String)

/** Detail of one item in a space. */
@Serializable
data class ItemRoute(val spaceId: String, val itemId: String)
