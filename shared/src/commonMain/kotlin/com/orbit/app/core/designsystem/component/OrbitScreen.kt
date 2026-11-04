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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.roundToInt

/** Room under the content of a scrolling screen so its last panel clears what floats over it. */
private val FloatingClearance = 104.dp

// The one decoration: a soft planet past the top right corner, behind the header.
private val PlanetRadius = 180.dp
private val PlanetRight = 150.dp
private val PlanetTop = 130.dp
private val HaloWidth = 36.dp

/**
 * Base layout of every screen: the space's colour as the page, the planet behind the header, the scaffold, the screen
 * gutter, the gap between panels and the scrolling. The gutter belongs here, so nothing touches the edge of the screen
 * by accident: only what is passed as [topBar] or [bottomBar] can. [topBar] is usually an [OrbitTopBar] or an
 * [OrbitColorHeader], [bottomBar] an [OrbitNavigationBar] and [floatingActionButton] an [OrbitAddButton]. A [scrollable]
 * screen scrolls its content, above the keyboard, and leaves room for what floats. Leave it off for a screen whose
 * content scrolls itself. [list] does the same for a lazy list, so what it shows can glide when it changes: pass it
 * instead of [content]. [primaryAction], such as the one button of a form, stays at the bottom in the gutter, in place
 * of a [bottomBar]. With [collapsingTopBar] the top bar slides away as the content scrolls down and comes back as soon
 * as it scrolls up, for a long screen whose way back should not hold room.
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
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val accent = OrbitTheme.accent
    val page by animateColorAsState(accent.page, OrbitMotion.effects())
    val planet by animateColorAsState(accent.planet, OrbitMotion.effects())
    val halo by animateColorAsState(accent.halo, OrbitMotion.effects())
    val collapse = remember { TopBarCollapse() }
    collapse.kept = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    Box(
        modifier.fillMaxSize().background(page).drawBehind {
            val radius = PlanetRadius.toPx()
            val center = Offset(size.width + PlanetRight.toPx() - radius, radius - PlanetTop.toPx())
            drawCircle(halo, radius + HaloWidth.toPx(), center)
            drawCircle(planet, radius, center)
        }.then(if (collapsingTopBar) Modifier.nestedScroll(collapse) else Modifier),
    ) {
        Scaffold(
            topBar = if (collapsingTopBar) {
                { Box(Modifier.background(page).collapsing(collapse)) { topBar() } }
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

/** Hides the top bar as the content scrolls down and shows it again as soon as it scrolls up, as M3's enterAlways does. */
private class TopBarCollapse : NestedScrollConnection {
    /** The bar's full height, measured by [collapsing]. */
    var height = 0f

    /** What stays of it: the status bar's height, so the bar's own colour stays behind the clock and battery. */
    var kept = 0f

    /** How far the bar has slid up, from 0 to -([height] - [kept]). */
    var offset by mutableFloatStateOf(0f)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val before = offset
        offset = (offset + available.y).coerceIn(-(height - kept).coerceAtLeast(0f), 0f)
        return Offset(0f, offset - before)
    }
}

/** Lays the bar out as tall as what is left of it, its bottom showing, so the content below follows it up. */
private fun Modifier.collapsing(collapse: TopBarCollapse): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    collapse.height = placeable.height.toFloat()
    val offset = collapse.offset.roundToInt()
    layout(placeable.width, (placeable.height + offset).coerceAtLeast(0)) { placeable.place(0, offset) }
}
