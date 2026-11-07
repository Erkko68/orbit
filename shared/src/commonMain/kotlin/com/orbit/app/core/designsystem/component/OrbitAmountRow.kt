package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
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
    supportingEmphasis: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    OrbitGroupRow(modifier, onClick = onClick) {
        Column(
            modifier = Modifier.weight(1f).padding(end = OrbitSpacing.sm).padding(vertical = OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = date,
                    modifier = Modifier.weight(0.65f).alignByBaseline(),
                    style = OrbitTheme.data.small,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                val emphasisStyle =
                    SpanStyle(fontWeight = MaterialTheme.typography.titleLarge.fontWeight, color = scheme.onSurface)
                Text(
                    text = buildAnnotatedString {
                        append(supporting)
                        val start = supportingEmphasis?.let { supporting.lastIndexOf(it) } ?: -1
                        if (start >= 0) addStyle(
                            emphasisStyle,
                            start,
                            start + requireNotNull(supportingEmphasis).length
                        )
                    },
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = SUPPORTING_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = amount,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = OrbitTheme.data.large,
                    color = scheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
