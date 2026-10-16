package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.ic_check
import orbit.shared.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

// Explicit dimensions from the approved mockup: a 24dp mark, 8dp theme corners and 2dp outline.
private val CheckboxMarkSize = 24.dp
private val CheckboxOutlineWidth = 2.dp

/** A rounded brand checkbox on M3 Surface, with a minimum 48dp target and accessible check state. */
@Composable
fun OrbitCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = if (onCheckedChange == null) {
        Modifier.semantics {
            role = Role.Checkbox
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        }
    } else {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    }
    Box(modifier.then(interaction).minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(CheckboxMarkSize),
            shape = MaterialTheme.shapes.extraSmall,
            color = when {
                !enabled -> scheme.surfaceContainerHighest
                checked -> scheme.secondary
                else -> scheme.surface
            },
            border = if (checked) null else BorderStroke(CheckboxOutlineWidth, scheme.onSurfaceVariant),
        ) {
            if (checked) {
                Icon(
                    painterResource(Res.drawable.ic_check), contentDescription = null,
                    modifier = Modifier.padding(OrbitSpacing.xs),
                    tint = if (enabled) scheme.onSecondary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}
