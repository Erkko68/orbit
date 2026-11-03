package com.orbit.app.core.speech

import androidx.compose.runtime.Composable

/**
 * The system prompts that let the app dictate: the microphone, and on iOS speech recognition too. Call the
 * returned function to show them; whether everything was allowed arrives in [onAnswer].
 */
@Composable
expect fun rememberDictationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit
