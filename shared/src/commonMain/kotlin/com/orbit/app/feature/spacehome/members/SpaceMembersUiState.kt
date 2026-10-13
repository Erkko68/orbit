package com.orbit.app.feature.spacehome.members

import org.jetbrains.compose.resources.StringResource

data class SpaceMembersUiState(val members: List<SpaceMember> = emptyList())

data class SpaceMember(val id: String, val name: String, val initials: String, val role: StringResource)
