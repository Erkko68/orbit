package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.InkSoft
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.Paper
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_remove
import org.jetbrains.compose.resources.painterResource

private val SignSize = 24.dp
private val SignIcon = 16.dp

/** Which way money goes for the person of a row: they get it back, or they owe it. */
enum class OrbitAmountSign { Plus, Minus }

/**
 * A row of an [OrbitGroup] about money, as Tricount lists it: [title] over [supporting], the [amount] at the end in the
 * data type, and a [leading] face when it is about a person. With a [sign] the whole row is a vivid fill, teal for money
 * to get back (+) and coral for money owed (−), with the sign before the amount, so colour never says it alone. Tapping
 * the row opens it when [onClick] is set. [done] tints it as a ticked row: money that has already moved, such as a
 * reimbursement in the ledger.
 */
@Composable
fun OrbitAmountRow(
    title: String,
    supporting: String,
    amount: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    sign: OrbitAmountSign? = null,
    done: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = when (sign) {
        OrbitAmountSign.Plus -> OrbitTheme.colors.credit
        OrbitAmountSign.Minus -> OrbitTheme.colors.debit
        null -> null
    }
    OrbitGroupRow(modifier.semantics(mergeDescendants = true) {}, onClick = onClick, done = done, fill = fill) {
        leading?.invoke()
        Column(Modifier.weight(1f).padding(vertical = OrbitSpacing.xs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (fill != null) Ink else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = if (fill != null) InkSoft else scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.padding(end = OrbitSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sign?.let { AmountSign(it) }
            Text(text = amount, style = OrbitTheme.data.large, color = if (fill != null) Ink else scheme.onSurface, softWrap = false)
        }
    }
}

@Composable
private fun AmountSign(sign: OrbitAmountSign) {
    Box(Modifier.size(SignSize).background(Ink, CircleShape), contentAlignment = Alignment.Center) {
        val icon = if (sign == OrbitAmountSign.Plus) Res.drawable.ic_add else Res.drawable.ic_remove
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(SignIcon), tint = Paper)
    }
}
