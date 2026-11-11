package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_badge_option
import orbit.shared.generated.resources.space_settings_colour
import org.jetbrains.compose.resources.stringResource

/**
 * The badge of a space, under its label: [OrbitColorPicker] on the space fills, each in the shape its tile shows, with
 * [accent] the one picked (#293). The stored index is the same one the space's colour used to be.
 */
@Composable
fun OrbitSpaceColorPicker(accent: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = OrbitTheme.colors.spaceAccents
    val names = palette.indices.map { stringResource(Res.string.space_badge_option, it + 1) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Text(
            text = stringResource(Res.string.space_settings_colour),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrbitColorPicker(
            colors = palette,
            labels = names,
            selectedIndex = accent.mod(palette.size),
            onSelect = onSelect,
            shapes = palette.indices.map(::memberShape),
        )
    }
}
