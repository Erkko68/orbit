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
import orbit.shared.generated.resources.space_colour_blue
import orbit.shared.generated.resources.space_colour_coral
import orbit.shared.generated.resources.space_colour_pink
import orbit.shared.generated.resources.space_colour_sun
import orbit.shared.generated.resources.space_colour_teal
import orbit.shared.generated.resources.space_colour_violet
import orbit.shared.generated.resources.space_settings_colour
import org.jetbrains.compose.resources.stringResource

/** The colour of a space, under its label: [OrbitColorPicker] on the palette, with [accent] the one picked. */
@Composable
fun OrbitSpaceColorPicker(accent: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val palette = OrbitTheme.colors.spaceAccents
    // One name per colour of the palette, in its order.
    val names = listOf(
        stringResource(Res.string.space_colour_violet),
        stringResource(Res.string.space_colour_coral),
        stringResource(Res.string.space_colour_teal),
        stringResource(Res.string.space_colour_sun),
        stringResource(Res.string.space_colour_blue),
        stringResource(Res.string.space_colour_pink),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Text(
            text = stringResource(Res.string.space_settings_colour),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OrbitColorPicker(colors = palette, labels = names, selectedIndex = accent.mod(palette.size), onSelect = onSelect)
    }
}
