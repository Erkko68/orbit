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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme

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
 * instead of [content].
 */
@Composable
fun OrbitScreen(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.sm),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollable: Boolean = false,
    list: (LazyListScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val accent = OrbitTheme.accent
    val page by animateColorAsState(accent.page, OrbitMotion.effects())
    val planet by animateColorAsState(accent.planet, OrbitMotion.effects())
    val halo by animateColorAsState(accent.halo, OrbitMotion.effects())
    Box(
        modifier.fillMaxSize().background(page).drawBehind {
            val radius = PlanetRadius.toPx()
            val center = Offset(size.width + PlanetRight.toPx() - radius, radius - PlanetTop.toPx())
            drawCircle(halo, radius + HaloWidth.toPx(), center)
            drawCircle(planet, radius, center)
        },
    ) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
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
