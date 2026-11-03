package com.orbit.app.feature.assistant.consent

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ai_consent_accept
import orbit.shared.generated.resources.ai_consent_body
import orbit.shared.generated.resources.ai_consent_failed
import orbit.shared.generated.resources.ai_consent_manual
import orbit.shared.generated.resources.ai_consent_nothing_saved
import orbit.shared.generated.resources.ai_consent_title
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.or
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
        // Closing the sheet is the manual path, like its tonal button.
        OrbitSheet(
            title = stringResource(Res.string.ai_consent_title),
            closeDescription = stringResource(Res.string.close),
            onDismiss = viewModel::decline,
        ) {
            Text(
                text = stringResource(Res.string.ai_consent_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(Res.string.ai_consent_nothing_saved), style = MaterialTheme.typography.bodyLarge)
            if (state.acceptFailed) OrbitErrorText(stringResource(Res.string.ai_consent_failed))
            OrbitButton(
                text = stringResource(Res.string.ai_consent_accept),
                onClick = viewModel::accept,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.submitting,
            )
            OrbitLabeledDivider(stringResource(Res.string.or))
            OrbitTonalButton(
                text = stringResource(Res.string.ai_consent_manual),
                onClick = viewModel::decline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
