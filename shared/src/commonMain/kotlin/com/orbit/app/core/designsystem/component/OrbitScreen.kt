package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/** Room under the content of a scrolling screen so its last section clears the floating action button. */
private val FabClearance = 88.dp

/**
 * Base layout of every screen: owns the scaffold, the screen gutter, the gap between sections and
 * the scrolling. [topBar] is usually an [OrbitTopBar], drawn edge to edge above the gutter, and
 * [floatingActionButton] an [OrbitExtendedFab]. A [scrollable] screen scrolls its content, padded
 * above and below, and leaves room for the floating action button. Leave it off for a screen whose
 * content scrolls itself, such as a lazy list.
 */
@Composable
fun OrbitScreen(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.md),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier, topBar = topBar, floatingActionButton = floatingActionButton ?: {}) { padding ->
        val scroll = if (scrollable) {
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = OrbitSpacing.lg, bottom = if (floatingActionButton == null) OrbitSpacing.lg else FabClearance)
        } else {
            Modifier
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).then(scroll).padding(horizontal = OrbitSpacing.md),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
