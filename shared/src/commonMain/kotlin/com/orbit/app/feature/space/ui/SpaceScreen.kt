package com.orbit.app.feature.space.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitHeading
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitScreenHeader
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.space.SpaceViewModel
import com.orbit.app.feature.space.card.SpaceCard
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_link
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.spaces_create
import orbit.shared.generated.resources.spaces_create_failed
import orbit.shared.generated.resources.spaces_create_name
import orbit.shared.generated.resources.spaces_create_subtitle
import orbit.shared.generated.resources.spaces_create_title
import orbit.shared.generated.resources.spaces_empty
import orbit.shared.generated.resources.spaces_join
import orbit.shared.generated.resources.spaces_join_code
import orbit.shared.generated.resources.spaces_join_subtitle
import orbit.shared.generated.resources.spaces_join_title
import orbit.shared.generated.resources.spaces_subtitle
import orbit.shared.generated.resources.spaces_title
import orbit.shared.generated.resources.spaces_yours
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's spaces, one [SpaceCard] each, and the "Create or join a space" mockup. Tapping
 * a space calls [onOpenSpace] with its id. Joining with an invite is a disabled placeholder until invites land.
 */
@Composable
fun SpaceScreen(onOpenSpace: (String) -> Unit, viewModel: SpaceViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg), scrollable = true) {
        OrbitScreenHeader(stringResource(Res.string.spaces_title), stringResource(Res.string.spaces_subtitle))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            Text(stringResource(Res.string.spaces_yours), style = MaterialTheme.typography.titleMedium)
            if (state.spaceIds.isEmpty()) {
                Text(
                    text = stringResource(Res.string.spaces_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (spaceId in state.spaceIds) {
                key(spaceId) {
                    SpaceCard(spaceId, onClick = { onOpenSpace(spaceId) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        OrbitCard(Modifier.fillMaxWidth()) {
            OrbitHeading(
                title = stringResource(Res.string.spaces_create_title),
                subtitle = stringResource(Res.string.spaces_create_subtitle),
            )
            OrbitTextField(
                value = state.newSpaceName,
                onValueChange = viewModel::onNewSpaceNameChange,
                placeholder = stringResource(Res.string.spaces_create_name),
                modifier = Modifier.fillMaxWidth(),
                isError = state.createFailed,
                supportingText = if (state.createFailed) stringResource(Res.string.spaces_create_failed) else null,
                leadingIcon = { Icon(painterResource(Res.drawable.ic_home), contentDescription = null) },
            )
            OrbitButton(
                text = stringResource(Res.string.spaces_create),
                onClick = viewModel::createSpace,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canCreate,
            )
        }
        OrbitLabeledDivider(stringResource(Res.string.or))
        OrbitCard(Modifier.fillMaxWidth()) {
            OrbitHeading(
                title = stringResource(Res.string.spaces_join_title),
                subtitle = stringResource(Res.string.spaces_join_subtitle),
            )
            OrbitTextField(
                value = "",
                onValueChange = {},
                placeholder = stringResource(Res.string.spaces_join_code),
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                leadingIcon = { Icon(painterResource(Res.drawable.ic_link), contentDescription = null) },
            )
            OrbitOutlinedButton(
                text = stringResource(Res.string.spaces_join),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
