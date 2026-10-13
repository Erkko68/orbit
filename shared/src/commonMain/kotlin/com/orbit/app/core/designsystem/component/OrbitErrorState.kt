package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

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
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OrbitOutlinedButton(retryText, onRetry, Modifier.fillMaxWidth())
    }
}
