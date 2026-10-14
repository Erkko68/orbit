package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** A searchable list of choices. Query, options and selection all belong to the caller's state. */
@Composable
fun OrbitSelectionDialog(
    title: String,
    dismissText: String,
    searchLabel: String,
    emptyText: String,
    query: String,
    options: List<OrbitSelectionOption>,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitDialog(title = title, dismissText = dismissText, onDismiss = onDismiss, modifier = modifier) {
        OrbitTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = searchLabel,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabel },
        )
        if (options.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = OrbitSpacing.xl * 8),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                items(options, key = { it.value }) { option ->
                    Surface(
                        color = if (option.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = option.selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.value) },
                        ),
                    ) {
                        Row(
                            modifier = Modifier.height(ControlHeight).padding(horizontal = OrbitSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                        ) {
                            Text(
                                option.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            RadioButton(selected = option.selected, onClick = null)
                        }
                    }
                }
            }
        }
    }
}
