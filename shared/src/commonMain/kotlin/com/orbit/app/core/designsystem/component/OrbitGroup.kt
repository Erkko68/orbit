package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

private val GroupGap = 3.dp
private val OuterCorner = 28.dp
private val InnerCorner = 8.dp
private val RowHeight = 74.dp

private enum class GroupPosition(val top: Boolean, val bottom: Boolean) {
    ONLY(true, true),
    FIRST(true, false),
    MIDDLE(false, false),
    LAST(false, true),
}

private fun positionIn(size: Int, index: Int) = when {
    size == 1 -> GroupPosition.ONLY
    index == 0 -> GroupPosition.FIRST
    index == size - 1 -> GroupPosition.LAST
    else -> GroupPosition.MIDDLE
}

private val LocalGroupPosition = staticCompositionLocalOf { GroupPosition.ONLY }

/**
 * Rows that read as one thing: small gaps, tight corners where two rows meet and big ones at the ends of the group.
 * [row] draws each of [items], usually as an [OrbitGroupRow]. [key] identifies an item, so a
 * row keeps its state when the list changes.
 */
@Composable
fun <T> OrbitGroup(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        items.forEachIndexed { index, item ->
            key(key(item)) {
                CompositionLocalProvider(LocalGroupPosition provides positionIn(items.size, index)) { row(item) }
            }
        }
    }
}

/** How something moves in a lazy list when the list changes: it glides to its new place, and what comes or goes fades. */
fun LazyItemScope.orbitItem(modifier: Modifier = Modifier): Modifier =
    modifier.animateItem(OrbitMotion.effects(), OrbitMotion.spatial(), OrbitMotion.effects())

/**
 * An [OrbitGroup] as items of a lazy list. A row whose [key] moves from one group to another stays the same row: it keeps
 * its state and glides there, with its corners following its new place.
 */
fun <T : Any> LazyListScope.orbitGroup(items: List<T>, key: (T) -> Any, row: @Composable (T) -> Unit) {
    itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
        Box(orbitItem(Modifier.padding(top = if (index == 0) 0.dp else GroupGap))) {
            CompositionLocalProvider(LocalGroupPosition provides positionIn(items.size, index)) { row(item) }
        }
    }
}

/** The outline of a row at its place in the [OrbitGroup]: tight where rows meet, and rounded off while [interaction] presses it. */
@Composable
internal fun groupShape(interaction: MutableInteractionSource): Shape {
    val position = LocalGroupPosition.current
    val pressed by interaction.collectIsPressedAsState()
    val top by animateDpAsState(if (pressed || position.top) OuterCorner else InnerCorner, OrbitMotion.spatial())
    val bottom by animateDpAsState(if (pressed || position.bottom) OuterCorner else InnerCorner, OrbitMotion.spatial())
    return RoundedCornerShape(top, top, bottom, bottom)
}

/**
 * One row of an [OrbitGroup]: a pressed row rounds off, and [done] tints it. Its fill is a panel, or the sunk fill on
 * what is already a panel, such as a sheet, or a vivid [fill] that carries ink, such as a balance. Tappable when
 * [onClick] is set.
 */
@Composable
fun OrbitGroupRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    done: Boolean = false,
    fill: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = groupShape(interaction)
    val colors = MaterialTheme.colorScheme
    val rest = if (LocalInsideCard.current) colors.surfaceContainer else colors.surfaceBright
    val shown by animateColorAsState(fill ?: if (done) OrbitTheme.accent.done else rest, OrbitMotion.effects())
    val contentColor = if (fill != null) Ink else colors.onSurface
    val body: @Composable () -> Unit = {
        CompositionLocalProvider(LocalInsideCard provides true) {
            Row(
                modifier = Modifier
                    .heightIn(min = RowHeight)
                    .padding(start = OrbitSpacing.md, end = OrbitSpacing.sm)
                    .padding(vertical = OrbitSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
    if (onClick == null) {
        Surface(modifier, shape = shape, color = shown, contentColor = contentColor, content = body)
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = shown,
            contentColor = contentColor,
            interactionSource = interaction,
            content = body,
        )
    }
}
