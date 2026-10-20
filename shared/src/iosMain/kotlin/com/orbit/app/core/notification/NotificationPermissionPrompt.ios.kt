package com.orbit.app.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** The system prompt, shown once per install: after that iOS answers with the stored choice at once. */
@Composable
actual fun rememberNotificationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit {
    val currentOnAnswer by rememberUpdatedState(onAnswer)
    return remember {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { granted, error ->
                error?.let { Logger.e { "Notification permission not asked: ${it.localizedDescription}" } }
                // The answer comes on a background queue; the screen takes it on the main one.
                dispatch_async(dispatch_get_main_queue()) { currentOnAnswer(granted) }
            }
        }
    }
}
