package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.roundToInt

/** Room under the content of a scrolling screen so its last panel clears what floats over it. */
private val FloatingClearance = 104.dp

// The one decoration: a soft planet past the top right corner, behind the header.

/**
 * Base layout of every screen: the flat page, the scaffold, the screen
 * gutter, the gap between panels and the scrolling. The gutter belongs here, so nothing touches the edge of the screen
 * by accident: only what is passed as [topBar] or [bottomBar] can. [topBar] is usually an [OrbitTopBar] or an
 * [OrbitColorHeader], [bottomBar] an [OrbitNavigationBar] and [floatingActionButton] an [OrbitAddButton]. A [scrollable]
 * screen scrolls its content, above the keyboard, and leaves room for what floats. Leave it off for a screen whose
 * content scrolls itself. [list] does the same for a lazy list, so what it shows can glide when it changes: pass it
 * instead of [content]. A [chat] list starts from the bottom and stays there as it grows, newest first in [list], so
 * the last message sits over the [bottomBar], such as an [OrbitChatComposer]. [primaryAction], such as the one button
 * of a form, stays at the bottom in the gutter, in place of a [bottomBar]. With [collapsingTopBar] the part of the top
 * bar marked [topBarCollapsible] shrinks away as the content scrolls down and comes back once it is scrolled to the
 * top, as M3's exitUntilCollapsed does: the rest, such as the way back, stays pinned.
 */
@Composable
fun OrbitScreen(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.sm),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    primaryAction: (@Composable () -> Unit)? = null,
    collapsingTopBar: Boolean = false,
    scrollable: Boolean = false,
    list: (LazyListScope.() -> Unit)? = null,
    chat: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val page by animateColorAsState(OrbitTheme.accent.page, OrbitMotion.effects())
    val collapse = remember { TopBarCollapse() }
    Box(
        modifier.fillMaxSize().background(page).then(if (collapsingTopBar) Modifier.nestedScroll(collapse) else Modifier),
    ) {
        Scaffold(
            topBar = if (collapsingTopBar) {
                { CompositionLocalProvider(LocalTopBarCollapse provides collapse) { topBar() } }
            } else {
                topBar
            },
            bottomBar = if (primaryAction == null) {
                bottomBar
            } else {
                {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
                    ) { primaryAction() }
                }
            },
            floatingActionButton = floatingActionButton ?: {},
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            val direction = LocalLayoutDirection.current
            val clearance = if (floatingActionButton == null) OrbitSpacing.lg else FloatingClearance
            if (list != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().consumeWindowInsets(padding).imePadding(),
                    reverseLayout = chat,
                    verticalArrangement = if (chat) {
                        Arrangement.spacedBy(OrbitSpacing.md, Alignment.Bottom)
                    } else {
                        Arrangement.Top
                    },
                    contentPadding = PaddingValues(
                        start = padding.calculateStartPadding(direction) + OrbitSpacing.md,
                        top = padding.calculateTopPadding() + OrbitSpacing.sm,
                        end = padding.calculateEndPadding(direction) + OrbitSpacing.md,
                        bottom = padding.calculateBottomPadding() + clearance,
                    ),
                    content = list,
                )
                return@Scaffold
            }
            val scroll = if (scrollable) {
                Modifier
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = OrbitSpacing.sm, bottom = clearance)
            } else {
                Modifier
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).then(scroll).padding(horizontal = OrbitSpacing.md),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}

/** The collapse of the screen's top bar, while [OrbitScreen] has one. */
private val LocalTopBarCollapse = staticCompositionLocalOf<TopBarCollapse?> { null }

/**
 * Shrinks the part of the top bar the content collapses, from scrolling down until it is gone, and gives it back once
 * the content is scrolled to its top, as M3's exitUntilCollapsed does.
 */
private class TopBarCollapse : NestedScrollConnection {
    /** The full height of the part that collapses, measured by [topBarCollapsible]. */
    var height = 0f

    /** How much of it is gone, from 0 to -[height]. */
    var offset by mutableFloatStateOf(0f)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0) slide(available.y) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y > 0) slide(available.y) else Offset.Zero

    private fun slide(by: Float): Offset {
        val before = offset
        offset = (offset + by).coerceIn(-height, 0f)
        return Offset(0f, offset - before)
    }
}

/**
 * Marks the part of a top bar that collapses on a screen with `collapsingTopBar`, such as a big title: it slides up
 * under what is above it and fades, and the bar shrinks with it. Elsewhere it does nothing.
 */
@Composable
internal fun Modifier.topBarCollapsible(): Modifier {
    val collapse = LocalTopBarCollapse.current ?: return this
    return clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            collapse.height = placeable.height.toFloat()
            val offset = collapse.offset.roundToInt()
            layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) { placeable.place(0, offset) }
        }
        .graphicsLayer { alpha = if (collapse.height == 0f) 1f else 1f + collapse.offset / collapse.height }
}
