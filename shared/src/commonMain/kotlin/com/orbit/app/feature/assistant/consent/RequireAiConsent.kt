package com.orbit.app.feature.assistant.consent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ai_consent_accept
import orbit.shared.generated.resources.ai_consent_body
import orbit.shared.generated.resources.ai_consent_failed
import orbit.shared.generated.resources.ai_consent_manual
import orbit.shared.generated.resources.ai_consent_nothing_saved
import orbit.shared.generated.resources.ai_consent_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Makes sure the user accepted what the assistant sends to the AI provider, then reports whether they go on
 * with it. Show it when the user opens the assistant, and remove it once [onResult] is called: true opens
 * the assistant, false is the manual path. A user who accepted before is never asked again.
 */
@Composable
fun RequireAiConsent(
    onResult: (accepted: Boolean) -> Unit,
    viewModel: AiConsentViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnResult by rememberUpdatedState(onResult)

    LaunchedEffect(state.result) {
        state.result?.let { accepted ->
            viewModel.onResultDelivered()
            currentOnResult(accepted)
        }
    }

    if (state.asking) {
        OrbitDialog(
            title = stringResource(Res.string.ai_consent_title),
            dismissText = stringResource(Res.string.ai_consent_manual),
            onDismiss = viewModel::decline,
            confirmText = stringResource(Res.string.ai_consent_accept),
            onConfirm = viewModel::accept,
            confirmEnabled = !state.submitting,
        ) {
            Text(stringResource(Res.string.ai_consent_body), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.ai_consent_nothing_saved), style = MaterialTheme.typography.bodyMedium)
            if (state.acceptFailed) OrbitErrorText(stringResource(Res.string.ai_consent_failed))
        }
    }
}
