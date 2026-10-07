package com.orbit.app.feature.space.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.space.SpaceViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.spaces_empty
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpaceScreen(viewModel: SpaceViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (state.spaces.isEmpty()) {
            Text(stringResource(Res.string.spaces_empty), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
