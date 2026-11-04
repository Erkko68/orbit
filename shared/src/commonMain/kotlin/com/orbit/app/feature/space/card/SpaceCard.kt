package com.orbit.app.feature.space.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitFace
import com.orbit.app.core.designsystem.component.OrbitSpaceTile
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_headline_due
import orbit.shared.generated.resources.space_items_open
import orbit.shared.generated.resources.spaces_all_clear
import orbit.shared.generated.resources.spaces_members
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One of the user's spaces as a card of its own colour, loaded by its own view model: its name, what the user has to do
 * there and its members' faces.
 */
@Composable
internal fun SpaceCard(
    spaceId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceCardViewModel = koinViewModel(key = "space-card-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = OrbitTheme.colors.accent(state.accent)
    OrbitSpaceTile(
        name = state.name,
        line = spaceLine(state),
        detail = spaceDetail(state),
        color = accent.vivid,
        partner = accent.partner,
        shape = memberShape(state.accent),
        faces = state.faces.mapIndexed { index, face ->
            OrbitFace(face.id, face.name, face.initials, Ink, memberShape(index), face.photoUrl, accent.vivid)
        },
        onClick = onClick,
        modifier = modifier,
    )
}

/** What is the user's today, what is open, or the size of the space until the items arrive. */
@Composable
private fun spaceLine(state: SpaceCardUiState): String {
    val due = state.dueToday
    val open = state.openCount
    return when {
        due == null || open == null ->
            pluralStringResource(Res.plurals.spaces_members, state.memberCount, state.memberCount)
        due > 0 -> pluralStringResource(Res.plurals.space_headline_due, due, due)
        open > 0 -> pluralStringResource(Res.plurals.space_items_open, open, open)
        else -> stringResource(Res.string.spaces_all_clear)
    }
}

/** What is open in the whole space, when the line above is about the user. */
@Composable
private fun spaceDetail(state: SpaceCardUiState): String? {
    val open = state.openCount ?: return null
    if ((state.dueToday ?: 0) == 0 || open == 0) return null
    return pluralStringResource(Res.plurals.space_items_open, open, open).uppercase()
}
