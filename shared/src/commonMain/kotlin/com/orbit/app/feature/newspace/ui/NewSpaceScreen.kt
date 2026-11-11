package com.orbit.app.feature.newspace.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitHero
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitMark
import com.orbit.app.core.designsystem.component.OrbitMarkSize
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.feature.newspace.NewSpaceUiState
import com.orbit.app.feature.newspace.NewSpaceViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_close
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_link
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.spaces_create
import orbit.shared.generated.resources.spaces_create_failed
import orbit.shared.generated.resources.spaces_create_name
import orbit.shared.generated.resources.spaces_join
import orbit.shared.generated.resources.spaces_join_code
import orbit.shared.generated.resources.spaces_join_title
import orbit.shared.generated.resources.spaces_new
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Two ways in to a space, on a page of their own: name one and pick its colour, which the page takes on as a preview,
 * or type or paste an invitation code or link. Either way the user lands in the space, through [onOpenSpace].
 */
@Composable
fun NewSpaceScreen(
    onClose: () -> Unit,
    onOpenSpace: (String) -> Unit,
    viewModel: NewSpaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.openSpaceId) {
        state.openSpaceId?.let(onOpenSpace)
    }
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = "",
                navigationIcon = { OrbitIconButton(Res.drawable.ic_close, stringResource(Res.string.close), onClose) },
            )
        },
        scrollable = true,
    ) {
        OrbitMark(size = OrbitMarkSize.Large)
        OrbitHero(stringResource(Res.string.spaces_new), Modifier.fillMaxWidth())
        CreateCard(state, viewModel)
        OrbitLabeledDivider(stringResource(Res.string.or))
        JoinCard(state, viewModel)
    }
}

@Composable
private fun CreateCard(state: NewSpaceUiState, viewModel: NewSpaceViewModel) {
    OrbitCard(Modifier.fillMaxWidth()) {
        OrbitTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = stringResource(Res.string.spaces_create_name),
            modifier = Modifier.fillMaxWidth(),
            isError = state.createFailed,
            supportingText = if (state.createFailed) stringResource(Res.string.spaces_create_failed) else null,
            leadingIcon = Res.drawable.ic_home,
        )
        OrbitButton(
            text = stringResource(Res.string.spaces_create),
            onClick = viewModel::createSpace,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canCreate,
        )
    }
}

@Composable
private fun JoinCard(state: NewSpaceUiState, viewModel: NewSpaceViewModel) {
    OrbitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.spaces_join_title), style = MaterialTheme.typography.titleMedium)
        OrbitTextField(
            value = state.joinCode,
            onValueChange = viewModel::onJoinCodeChange,
            placeholder = stringResource(Res.string.spaces_join_code),
            modifier = Modifier.fillMaxWidth(),
            isError = state.joinError != null,
            supportingText = state.joinError?.let { stringResource(it.message()) },
            leadingIcon = Res.drawable.ic_link,
        )
        OrbitTonalButton(
            text = stringResource(Res.string.spaces_join),
            onClick = viewModel::joinSpace,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canJoin,
        )
    }
}
