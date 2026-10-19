package com.orbit.app.feature.space.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatarStack
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitIconBadge
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_right
import orbit.shared.generated.resources.spaces_members
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val VISIBLE_AVATARS = 3

/**
 * One of the user's spaces, loaded by its own view model: an entity card with a line about the space (its
 * accent and size, for now) over its name, an arrow to open it, and its members' avatars.
 */
@Composable
internal fun SpaceCard(
    spaceId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceCardViewModel = koinViewModel(key = "space-card-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitCard(modifier, onClick = onClick, elevated = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The space's accent, as a dot.
                    val accent = OrbitTheme.colors.spaceAccent(state.accent)
                    Box(Modifier.size(OrbitSpacing.sm).background(accent, CircleShape))
                    OrbitPill(pluralStringResource(Res.plurals.spaces_members, state.memberCount, state.memberCount))
                }
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OrbitIconBadge(Res.drawable.ic_chevron_right)
        }
        if (state.memberInitials.isNotEmpty()) {
            OrbitAvatarStack(state.memberInitials, max = VISIBLE_AVATARS)
        }
    }
}
