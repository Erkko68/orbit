package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private const val TITLE_LINES = 2
private const val SUPPORTING_LINES = 2

/** A monetary entry: its title, total amount, payer and regional date remain clear at large text sizes. */
@Composable
fun OrbitAmountRow(
    title: String,
    supporting: String,
    amount: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    OrbitGroupRow(modifier, onClick = onClick) {
        Column(
            modifier = Modifier.weight(1f).padding(vertical = OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                maxLines = TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = amount,
                style = OrbitTheme.data.large,
                color = scheme.onSurface,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = SUPPORTING_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date,
                style = OrbitTheme.data.small,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
