package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import orbit.shared.generated.resources.ic_orbit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val MarkSize = 40.dp

/**
 * Opens a screen that has no top bar, such as sign-in: the Orbit mark and name, then what the screen
 * is for in [title] and [subtitle], all centered.
 */
@Composable
fun OrbitScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_orbit),
                contentDescription = null,
                modifier = Modifier.size(MarkSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
