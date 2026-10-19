package com.orbit.app.core.notification.permission

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.notification.rememberNotificationPermissionPrompt
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.notification_permission_body
import orbit.shared.generated.resources.notification_permission_not_now
import orbit.shared.generated.resources.notification_permission_open_settings
import orbit.shared.generated.resources.notification_permission_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Makes sure the user allows notifications, then reports whether they do. Show it when the user turns on
 * something that notifies, never at launch, and remove it once [onResult] is called. If the user refuses
 * the system prompt, a dialog offers the system settings; coming back with notifications on counts as allowed.
 */
@Composable
fun RequireNotificationPermission(
    onResult: (allowed: Boolean) -> Unit,
    viewModel: NotificationPermissionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnResult by rememberUpdatedState(onResult)
    val prompt = rememberNotificationPermissionPrompt(viewModel::onAnswer)

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    LaunchedEffect(state.prompting) {
        if (state.prompting) {
            viewModel.onPromptShown()
            prompt()
        }
    }
    LaunchedEffect(state.result) {
        state.result?.let { allowed ->
            viewModel.onResultDelivered()
            currentOnResult(allowed)
        }
    }

    if (state.refused) {
        OrbitDialog(
            title = stringResource(Res.string.notification_permission_title),
            dismissText = stringResource(Res.string.notification_permission_not_now),
            onDismiss = viewModel::dismiss,
            confirmText = stringResource(Res.string.notification_permission_open_settings),
            onConfirm = viewModel::openSettings,
        ) {
            Text(
                text = stringResource(Res.string.notification_permission_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
