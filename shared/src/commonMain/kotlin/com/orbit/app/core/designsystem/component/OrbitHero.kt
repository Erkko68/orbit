package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

/**
 * The big bold opening of a screen, on the page: [title], and a [datum] over it when there is one, such as the date.
 */
@Composable
fun OrbitHero(title: String, modifier: Modifier = Modifier, datum: String? = null) {
    Column(
        modifier = modifier.padding(vertical = OrbitSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
    ) {
        datum?.let {
            Text(it.uppercase(), style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
