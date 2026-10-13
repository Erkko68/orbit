package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * A question and its answers as chips, such as one the assistant asks. It holds no state: the chips of
 * [selected] show as selected and a tap calls [onSelect] with the option. Whether that picks one answer
 * (replace the set) or several (toggle in it) is up to the caller. Full width, and the main content of
 * its screen, so it is elevated.
 */
@Composable
fun OrbitQuestionCard(
    prompt: String,
    options: List<String>,
    selected: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitCard(modifier.fillMaxWidth(), elevated = true) {
        Text(prompt, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            for (option in options) {
                OrbitFilterChip(option, selected = option in selected, onClick = { onSelect(option) })
            }
        }
    }
}
