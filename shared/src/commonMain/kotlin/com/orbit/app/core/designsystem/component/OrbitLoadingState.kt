package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

private val SpinnerSize = 24.dp
private val SpinnerStroke = 3.dp

/** What a list or a tab shows while it loads: a card with a spinner and [label] saying what is loading. */
@Composable
fun OrbitLoadingState(label: String, modifier: Modifier = Modifier) {
    OrbitCard(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(SpinnerSize), strokeWidth = SpinnerStroke)
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
