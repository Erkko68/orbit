package com.orbit.app.feature.space.card

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_right
import orbit.shared.generated.resources.spaces_members
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * One of the user's spaces, loaded by its own view model: its badge in the space's accent, its name
 * and size, and a chevron to open it.
 */
@Composable
internal fun SpaceCard(
    spaceId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceCardViewModel = koinViewModel(key = spaceId) { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accents = OrbitTheme.colors.spaceAccents
    OrbitCard(modifier, onClick = onClick) {
        OrbitListItem(
            title = state.name,
            supporting = pluralStringResource(Res.plurals.spaces_members, state.memberCount, state.memberCount),
            leading = { OrbitAvatar(state.name.take(1).uppercase(), accents[state.accent.mod(accents.size)]) },
            trailing = {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
