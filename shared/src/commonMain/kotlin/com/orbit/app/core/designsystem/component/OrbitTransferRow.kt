package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_forward
import org.jetbrains.compose.resources.painterResource

/** A proposed transfer: people above, amount and the explicit action aligned below. Wide text may wrap, never clip. */
@Composable
fun OrbitTransferRow(
    payer: String,
    payee: String,
    payerLabel: String,
    payeeLabel: String,
    amount: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OrbitGroupRow(modifier) {
        Column(
            Modifier.weight(1f).padding(end = OrbitSpacing.sm).padding(vertical = OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransferPerson(payerLabel, payer, TextAlign.Start, Modifier.weight(1f))
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(Res.drawable.ic_arrow_forward), contentDescription = null)
                }
                TransferPerson(payeeLabel, payee, TextAlign.End, Modifier.weight(1f))
            }
            TransferAction(amount, action, onAction, enabled)
        }
    }
}

/** Measure the action at the available row width before deciding whether both elements fit on one line. */
@Composable
private fun TransferAction(amount: String, action: String, onAction: () -> Unit, enabled: Boolean) {
    val gap = OrbitSpacing.sm
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(amount, style = OrbitTheme.data.large)
            OrbitTonalButton(
                action, onAction, enabled = enabled,
                shape = CircleShape,
                compact = true,
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val value = measurables[0].measure(loose)
        val button = measurables[1].measure(loose)
        val width = constraints.maxWidth
        val inline = value.width + gap.roundToPx() + button.width <= width
        val contentHeight =
            if (inline) maxOf(value.height, button.height) else value.height + gap.roundToPx() + button.height
        layout(width, constraints.constrainHeight(contentHeight)) {
            value.placeRelative(0, if (inline) (contentHeight - value.height) / 2 else 0)
            button.placeRelative(
                width - button.width,
                if (inline) (contentHeight - button.height) / 2 else value.height + gap.roundToPx()
            )
        }
    }
}

@Composable
private fun TransferPerson(label: String, name: String, textAlign: TextAlign, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = textAlign)
        Text(name, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = textAlign)
    }
}
