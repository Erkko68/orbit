package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_expand_more
import org.jetbrains.compose.resources.painterResource

private val TopBarHeight = 64.dp
private val ControlSize = 48.dp
private val ChevronSize = 18.dp

/**
 * Top of a screen, passed to [OrbitScreen]'s `topBar`. It sits on the page, with no fill of its own: an optional
 * [navigationIcon] (back), [title], big and after the Orbit mark when [showMark] is set, and [actions]. With [onTitleClick] the
 * title is a soft button with the mark and a chevron, which [onTitleClickLabel] names for screen readers, such as a
 * space's name that opens the list of spaces. Built on stable APIs because M3's `TopAppBar` is experimental. It pads
 * itself below the status bar.
 */
@Composable
fun OrbitTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showMark: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
    onTitleClickLabel: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val insets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .heightIn(min = TopBarHeight)
            .padding(horizontal = OrbitSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon?.invoke()
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (onTitleClick != null) {
                TitleButton(title, onTitleClick, onTitleClickLabel)
            } else {
                Row(
                    modifier = if (navigationIcon == null) Modifier.padding(start = OrbitSpacing.sm) else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showMark) {
                        OrbitMark(size = OrbitMarkSize.Medium)
                        BarTitle(title, style = MaterialTheme.typography.titleLarge)
                    } else {
                        BarTitle(title)
                    }
                }
            }
        }
        Row(Modifier.padding(end = OrbitSpacing.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
private fun BarTitle(
    title: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = title,
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The title that opens something: the mark, the name and a chevron on a soft pill, so it reads as something to change.
 * It takes at most half the screen, and a longer name ends in an ellipsis.
 */
@Composable
private fun TitleButton(title: String, onClick: () -> Unit, onClickLabel: String?) {
    val shape = MaterialTheme.shapes.medium
    val halfScreen = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.width / 2).toDp() }
    Surface(
        modifier = Modifier.padding(start = OrbitSpacing.sm).widthIn(max = halfScreen).panelShadow(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                .heightIn(min = ControlSize)
                .padding(start = OrbitSpacing.sm, end = OrbitSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitMark()
            BarTitle(title, Modifier.weight(1f, fill = false))
            Icon(painterResource(Res.drawable.ic_expand_more), contentDescription = null, Modifier.size(ChevronSize))
        }
    }
}
