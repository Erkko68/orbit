package com.orbit.app.domain.model

/**
 * What the assistant proposes at the end of an interview (#64): the new [space] when the interview was for one, and
 * the [items] to add. Only proposals that pass the same checks as manual input get here (#65), and nothing is saved
 * until the user confirms it (#76).
 */
data class Plan(val space: SpaceProposal?, val items: List<NewItem>)

/** A new space: its [name] and [accent], an index into the space colours as a [Space] stores it. */
data class SpaceProposal(val name: String, val accent: Int)

/** How many accents a space may take: the entries of `OrbitTheme.colors.spaceAccents`. */
const val SPACE_ACCENT_COUNT = 6
