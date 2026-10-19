package com.orbit.app.core.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** The Android 13+ runtime permission. Its launcher outlives the activity being recreated, unlike a plain callback. */
@Composable
actual fun rememberNotificationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // No prompt before Android 13: notifications are on unless the user turned them off in the settings.
        val currentOnAnswer by rememberUpdatedState(onAnswer)
        return remember { { currentOnAnswer(false) } }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onAnswer)
    return remember(launcher) { { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) } }
}
