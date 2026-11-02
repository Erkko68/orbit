package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One place of an [OrbitNavigationBar]. */
data class OrbitNavigationDestination(val label: String, val icon: DrawableResource)

/**
 * The places of a space, such as its home, tasks and expenses, passed to [OrbitScreen]'s `bottomBar`. The selected
 * one's icon sits on a primary pill, like the selected [OrbitSegmentedTabs] option. It pads itself above the system
 * bar. The labels are set here because the default selected one is `secondary`, which fails AA as text.
 */
@Composable
fun OrbitNavigationBar(
    destinations: List<OrbitNavigationDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    NavigationBar(modifier = modifier, containerColor = scheme.surfaceContainer) {
        destinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onPrimary,
                    selectedTextColor = scheme.onSurface,
                    indicatorColor = scheme.primary,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                ),
            )
        }
    }
}
