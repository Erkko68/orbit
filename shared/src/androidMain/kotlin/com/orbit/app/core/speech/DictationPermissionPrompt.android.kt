package com.orbit.app.core.speech

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The microphone runtime permission. Its launcher outlives the activity being recreated, unlike a plain callback. */
@Composable
actual fun rememberDictationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onAnswer)
    return remember(launcher) { { launcher.launch(Manifest.permission.RECORD_AUDIO) } }
}
