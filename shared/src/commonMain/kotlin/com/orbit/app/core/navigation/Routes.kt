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

/** Home of one space, with its items, agenda and members tabs. */
@Serializable
data class SpaceHomeRoute(val spaceId: String)
