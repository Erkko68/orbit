package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.designsystem.theme.addCubics
import com.orbit.app.core.designsystem.theme.cubicsAt
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource

private val CheckTarget = 56.dp
private val CheckSize = 46.dp
private val RingWidth = 2.5.dp
private val CompactTarget = 48.dp
private val CompactSize = 30.dp
private val CompactRing = 2.dp
private val CompactTick = 18.dp

// The check travels one path: circle, squircle while pressed, cookie when done. Undoing it runs the path back.
private const val AT_REST = 0f
private const val PRESSED = 1f
private const val DONE = 2f
private val PressMorph by lazy { Morph(OrbitShape.Circle.polygon, OrbitShape.Squircle.polygon) }
private val DoneMorph by lazy { Morph(OrbitShape.Squircle.polygon, OrbitShape.Cookie.polygon) }

private fun checkPath(position: Float, size: Size) = Path().apply {
    addCubics(
        if (position <= PRESSED) PressMorph.cubicsAt(position) else DoneMorph.cubicsAt(position - PRESSED),
        size,
    )
}

/**
 * The one check of the app, the same on every list. Whether it is [checked] is the caller's state.
 * [contentDescription] says what tapping it does, such as "Complete Buy milk". Keeps a 56dp target. [compact] is the
 * same check for a row that holds little else, such as an entry of a checklist.
 */
@Composable
fun OrbitCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val position = animateFloatAsState(
        targetValue = when {
            checked -> DONE
            pressed -> PRESSED
            else -> AT_REST
        },
        animationSpec = OrbitMotion.spatial(),
    )
    val fill by animateColorAsState(
        if (checked) OrbitTheme.accent.vivid else MaterialTheme.colorScheme.surfaceBright,
        OrbitMotion.effects(),
    )
    val ring by animateColorAsState(
        if (checked) Color.Transparent else OrbitTheme.colors.inkTertiary,
        OrbitMotion.effects(),
    )
    Box(
        modifier = modifier
            .size(if (compact) CompactTarget else CheckTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(if (compact) CompactSize else CheckSize).drawBehind {
                val path = checkPath(position.value, size)
                drawPath(path, fill)
                drawPath(path, ring, style = Stroke((if (compact) CompactRing else RingWidth).toPx()))
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = Ink,
                modifier = (if (compact) Modifier.size(CompactTick) else Modifier).graphicsLayer {
                    val tick = (position.value - PRESSED).coerceIn(0f, 1f)
                    alpha = tick
                    scaleX = tick
                    scaleY = tick
                },
            )
        }
    }
}
