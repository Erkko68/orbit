package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The members of a space with their roles, loaded by the tab's own view model. */
@Composable
internal fun SpaceMembersTab(
    spaceId: String,
    modifier: Modifier = Modifier,
    viewModel: SpaceMembersViewModel = koinViewModel(key = spaceId) { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
        state.members.forEachIndexed { index, member ->
            OrbitListItem(
                title = member.name,
                supporting = stringResource(member.role),
                leading = {
                    OrbitAvatar(member.initials, OrbitTheme.colors.spaceAccent(index), photoUrl = member.photoUrl)
                },
            )
        }
    }
}
