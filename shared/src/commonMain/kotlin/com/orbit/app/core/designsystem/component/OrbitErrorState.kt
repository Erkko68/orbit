package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * What a list or a tab shows when it could not load: a card saying what failed in [title] and what to
 * do in [body], with a [retryText] button that calls [onRetry]. A tonal action, because the screen's
 * one primary action stays where it was.
 */
@Composable
fun OrbitErrorState(
    title: String,
    body: String,
    retryText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitCard(modifier) {
        OrbitHeading(title, body, titleColor = MaterialTheme.colorScheme.error)
        OrbitOutlinedButton(retryText, onRetry, Modifier.fillMaxWidth())
    }
}
