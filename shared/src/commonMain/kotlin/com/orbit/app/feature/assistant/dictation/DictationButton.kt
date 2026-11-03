package com.orbit.app.feature.assistant.dictation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.speech.DictationFailure
import com.orbit.app.core.speech.rememberDictationPermissionPrompt
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.dictation_error
import orbit.shared.generated.resources.dictation_failed_title
import orbit.shared.generated.resources.dictation_no_permission
import orbit.shared.generated.resources.dictation_no_speech
import orbit.shared.generated.resources.dictation_start
import orbit.shared.generated.resources.dictation_stop
import orbit.shared.generated.resources.dictation_unavailable
import orbit.shared.generated.resources.ic_mic
import orbit.shared.generated.resources.ic_stop
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The microphone of a text answer, for its field's trailing icon. [onStart] runs when a dictation begins and
 * [onTranscript] with everything heard since, each time it grows: the field adds it after what was typed.
 * When the phone cannot dictate, a sheet says why and typing still works. [prompt] is the system one;
 * tests, which have no system to ask, replace it.
 */
@Composable
fun DictationButton(
    onStart: () -> Unit,
    onTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictationViewModel = koinViewModel(),
    prompt: () -> Unit = rememberDictationPermissionPrompt(viewModel::onPermissionAnswer),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnTranscript by rememberUpdatedState(onTranscript)

    LaunchedEffect(state.started) { if (state.started > 0) currentOnStart() }
    LaunchedEffect(state.transcript) { if (state.transcript.isNotEmpty()) currentOnTranscript(state.transcript) }
    LaunchedEffect(state.askingPermission) {
        if (state.askingPermission) {
            viewModel.onPermissionPromptShown()
            prompt()
        }
    }

    if (state.listening) {
        OrbitIconButton(Res.drawable.ic_stop, stringResource(Res.string.dictation_stop), viewModel::toggle, modifier)
    } else {
        OrbitIconButton(Res.drawable.ic_mic, stringResource(Res.string.dictation_start), viewModel::toggle, modifier)
    }

    state.failure?.let { failure ->
        OrbitSheet(
            title = stringResource(Res.string.dictation_failed_title),
            closeDescription = stringResource(Res.string.close),
            onDismiss = viewModel::dismissFailure,
        ) {
            Text(
                text = stringResource(failure.message()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun DictationFailure.message(): StringResource = when (this) {
    DictationFailure.NO_PERMISSION -> Res.string.dictation_no_permission
    DictationFailure.UNAVAILABLE -> Res.string.dictation_unavailable
    DictationFailure.NO_SPEECH -> Res.string.dictation_no_speech
    DictationFailure.ERROR -> Res.string.dictation_error
}
