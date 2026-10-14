package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource

/** A field that opens a choice list. It looks like the other fields but cannot be typed into. */
@Composable
fun OrbitSelectionField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Box(modifier) {
        OrbitTextField(
            value = value,
            onValueChange = {},
            placeholder = label,
            label = label,
            enabled = enabled,
            isError = isError,
            supportingText = supportingText,
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            trailingIcon = {
                Icon(painterResource(Res.drawable.ic_chevron_right), null, Modifier.rotate(90f))
            },
        )
        Box(
            Modifier.matchParentSize().clickable(enabled = enabled, onClick = onClick).semantics {
                contentDescription = label
                stateDescription = value
                text = AnnotatedString(value)
                role = Role.Button
            },
        )
    }
}
