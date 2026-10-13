package com.orbit.app.feature.space.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Space
import com.orbit.app.feature.space.SpaceViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.ic_chevron_right
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_link
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.spaces_create
import orbit.shared.generated.resources.spaces_create_name
import orbit.shared.generated.resources.spaces_create_subtitle
import orbit.shared.generated.resources.spaces_create_title
import orbit.shared.generated.resources.spaces_empty
import orbit.shared.generated.resources.spaces_join
import orbit.shared.generated.resources.spaces_join_code
import orbit.shared.generated.resources.spaces_join_subtitle
import orbit.shared.generated.resources.spaces_join_title
import orbit.shared.generated.resources.spaces_members
import orbit.shared.generated.resources.spaces_subtitle
import orbit.shared.generated.resources.spaces_title
import orbit.shared.generated.resources.spaces_yours
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's spaces and the "Create or join a space" mockup. Tapping a space calls
 * [onOpenSpace] with its id. Joining with an invite is a disabled placeholder until invites land.
 */
@Composable
fun SpaceScreen(onOpenSpace: (String) -> Unit, viewModel: SpaceViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    OrbitScreen {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = OrbitSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
            Heading(Res.string.spaces_title, Res.string.spaces_subtitle, MaterialTheme.typography.headlineMedium)
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                Text(stringResource(Res.string.spaces_yours), style = MaterialTheme.typography.titleMedium)
                if (state.spaces.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.spaces_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                for (space in state.spaces) {
                    SpaceRow(space, onClick = { onOpenSpace(space.id) })
                }
            }
            OrbitCard(Modifier.fillMaxWidth()) {
                Heading(Res.string.spaces_create_title, Res.string.spaces_create_subtitle)
                OrbitTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(Res.string.spaces_create_name),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_home), contentDescription = null) },
                )
                OrbitButton(
                    text = stringResource(Res.string.spaces_create),
                    onClick = {
                        viewModel.createSpace(name)
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank(),
                )
            }
            OrbitLabeledDivider(stringResource(Res.string.or))
            OrbitCard(Modifier.fillMaxWidth()) {
                Heading(Res.string.spaces_join_title, Res.string.spaces_join_subtitle)
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
}

/** One of the user's spaces: its badge in the space's accent, its name and size, and a chevron to open it. */
@Composable
private fun SpaceRow(space: Space, onClick: () -> Unit) {
    val accents = OrbitTheme.colors.spaceAccents
    OrbitCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitAvatar(space.name.take(1).uppercase(), accents[space.accent.mod(accents.size)])
            Column(Modifier.weight(1f)) {
                Text(space.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = pluralStringResource(Res.plurals.spaces_members, space.memberIds.size, space.memberIds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A title with a line of secondary text under it, in the main text color even on a card. */
@Composable
private fun Heading(
    title: StringResource,
    subtitle: StringResource,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Text(stringResource(title), style = titleStyle, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
