package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.designsystem.theme.PanelTopShape
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One place of an [OrbitNavigationBar]: a bold [icon], and the filled [selectedIcon] it turns into when selected. */
data class OrbitNavigationDestination(
    val label: String,
    val icon: DrawableResource,
    val selectedIcon: DrawableResource = icon,
)

/**
 * The places of a space, such as its home, tasks and expenses, passed to [OrbitScreen]'s `bottomBar`: M3's short bar,
 * flush with the bottom edge, with each icon beside its label. The selected one sits on the space's colour, its icon
 * fills and its ink darkens, so colour never carries the meaning alone. It pads itself above the system bar.
 */
@Composable
fun OrbitNavigationBar(
    destinations: List<OrbitNavigationDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val quiet = OrbitTheme.colors.inkTertiary
    Surface(modifier.panelShadow(PanelTopShape), shape = PanelTopShape, color = scheme.surfaceBright) {
        ShortNavigationBar(containerColor = Color.Transparent, arrangement = ShortNavigationBarArrangement.EqualWeight) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(index) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                        )
                    },
                    label = { Text(destination.label, style = MaterialTheme.typography.labelMedium) },
                    iconPosition = NavigationItemIconPosition.Start,
                    colors = ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onSurface,
                        selectedTextColor = scheme.onSurface,
                        selectedIndicatorColor = OrbitTheme.accent.selected,
                        unselectedIconColor = quiet,
                        unselectedTextColor = quiet,
                    ),
                )
            }
        }
    }
}
