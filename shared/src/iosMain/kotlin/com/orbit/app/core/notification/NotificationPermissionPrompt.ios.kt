package com.orbit.app.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger

/** Stub until #204 asks through UNUserNotificationCenter: the prompt answers no at once. */
@Composable
actual fun rememberNotificationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit {
    val currentOnAnswer by rememberUpdatedState(onAnswer)
    return remember {
        {
            Logger.w { "Notification permission not asked: iOS notifications land in #204" }
            currentOnAnswer(false)
        }
    }
}
