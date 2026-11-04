package com.orbit.app.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val BurstSize = 170.dp
private val BurstOffset = 54.dp
private val FaceClearance = 76.dp
private val ActionHeight = 56.dp
private val ActionCorner = 28.dp
private val ActionCornerPressed = 14.dp
private val ActionRing = 2.dp

/**
 * The one hero moment of the app: what is the user's to do now. Inverse fill, a burst in the partner colour, big type and one big action in the vivid one.
 * [label] says when, [title] what, and tapping the card calls [onOpen]. [actionText] is what [onAction] does, and once [finished] it
 * is the way back. [initials] and [photoUrl] are the user's. Use it once on a screen: the loud look only works if it stays rare.
 */
@Composable
fun OrbitTurnCard(
    label: String,
    title: String,
    actionText: String,
    finished: Boolean,
    onAction: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    initials: String? = null,
    photoUrl: String? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val vivid = OrbitTheme.accent.vivid
    Surface(
        onClick = onOpen,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.inverseSurface,
        contentColor = scheme.inverseOnSurface,
    ) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = BurstOffset, y = -BurstOffset)
                    .size(BurstSize)
                    .background(OrbitTheme.accent.partner, OrbitShape.Burst),
            )
            initials?.let {
                OrbitAvatar(
                    initials = it,
                    accent = Ink,
                    modifier = Modifier.align(Alignment.TopEnd).padding(OrbitSpacing.md),
                    photoUrl = photoUrl,
                    contentColor = vivid,
                    shape = OrbitShape.Cookie,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(OrbitSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            ) {
                Text(label.uppercase(), style = OrbitTheme.data.small)
                Text(
                    text = title,
                    modifier = Modifier.padding(end = FaceClearance),
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TurnAction(actionText, finished, enabled, onAction)
                }
            }
        }
    }
}

/** A big pill whose corners tighten while pressed. Once finished it steps back to a ring. */
@Composable
private fun TurnAction(text: String, finished: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) ActionCornerPressed else ActionCorner, OrbitMotion.spatial())
    val onInverse = MaterialTheme.colorScheme.inverseOnSurface
    val vivid = OrbitTheme.accent.vivid
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = ActionHeight),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (finished) Color.Transparent else vivid,
            contentColor = if (finished) onInverse else Ink,
            disabledContainerColor = if (finished) Color.Transparent else vivid,
            disabledContentColor = if (finished) onInverse else Ink,
        ),
        border = if (finished) BorderStroke(ActionRing, onInverse) else null,
        interactionSource = interaction,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
