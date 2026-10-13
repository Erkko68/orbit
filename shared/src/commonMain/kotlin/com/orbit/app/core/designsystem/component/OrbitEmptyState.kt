package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** What a list or a tab shows when it has nothing: a card saying so in [title] and what will fill it in [body]. */
@Composable
fun OrbitEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    OrbitCard(modifier) {
        OrbitHeading(title, body)
    }
}
