package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * Base layout of every screen: owns the scaffold, the screen gutter and the gap between sections.
 * [topBar] is usually an [OrbitTopBar], drawn edge to edge above the gutter.
 */
@Composable
fun OrbitScreen(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OrbitSpacing.md),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier, topBar = topBar) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = OrbitSpacing.md),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
