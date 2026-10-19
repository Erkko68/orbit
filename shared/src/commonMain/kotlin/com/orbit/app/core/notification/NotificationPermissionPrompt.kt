package com.orbit.app.core.notification

import androidx.compose.runtime.Composable

/**
 * The system prompt that asks the user to allow notifications. Call the returned function to show it;
 * the answer arrives in [onAnswer], even if the screen was recreated in between.
 */
@Composable
expect fun rememberNotificationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit
