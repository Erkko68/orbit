package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * A [title] with a line of secondary text under it, heading a card or a section. The title keeps the
 * main text color even on a card. [titleStyle] is a typography role, `titleMedium` unless the heading
 * opens a screen.
 */
@Composable
fun OrbitHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Text(title, style = titleStyle, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
