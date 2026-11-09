package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ActionHeight = 44.dp
private val ActionIcon = 18.dp

/**
 * A payment that settles a balance: the [faces] of who pays and who gets it, the [sentence] ("You owe Ana") over its
 * [amount]. When the user is the one who pays, a small filled [action] ("Pay") with its [actionIcon] sits at the end,
 * in the teal of a settled balance; without one the row only says what is coming. [onPaid] only asks: the payment is
 * recorded once it is confirmed. [actionDescription] names the action for screen readers.
 */
@Composable
fun OrbitTransferRow(
    faces: List<OrbitFace>,
    sentence: String,
    amount: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    actionIcon: DrawableResource? = null,
    actionDescription: String? = null,
    onPaid: () -> Unit = {},
    enabled: Boolean = true,
) {
    OrbitGroupRow(modifier) {
        OrbitFaceStack(faces)
        Column(Modifier.weight(1f).padding(vertical = OrbitSpacing.xs)) {
            Text(sentence, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(amount, style = OrbitTheme.data.large, color = MaterialTheme.colorScheme.onSurface)
        }
        if (action != null && actionIcon != null) {
            Surface(
                onClick = onPaid,
                modifier = Modifier.heightIn(min = ActionHeight).semantics { contentDescription = actionDescription ?: action },
                enabled = enabled,
                shape = CircleShape,
                color = OrbitTheme.colors.credit,
                contentColor = Ink,
            ) {
                Row(
                    Modifier.padding(horizontal = OrbitSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(actionIcon), contentDescription = null, modifier = Modifier.size(ActionIcon))
                    Text(action, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
