package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitShape
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_tune

private val CrewWidth = 132.dp
private val CrewHeight = 56.dp
private val CrewFace = 20.dp
private const val SETTINGS_SCALE = 0.75f
private const val SETTINGS = "settings"

/**
 * The people of a space floating in the top bar around a smaller round settings shape, which [settingsDescription] names.
 * They are one way into the space's settings: tapping any of them calls [onClick].
 */
@Composable
fun OrbitCrew(
    faces: List<OrbitFloatingFace>,
    settingsDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = OrbitFloatingFace(
        id = SETTINGS,
        initials = "",
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = OrbitShape.Circle,
        description = settingsDescription,
        contentColor = MaterialTheme.colorScheme.onSurface,
        icon = Res.drawable.ic_tune,
        scale = SETTINGS_SCALE,
    )
    OrbitFloatingFaces(
        faces = listOf(settings) + faces,
        modifier = modifier.size(CrewWidth, CrewHeight),
        largest = CrewFace,
        onFaceClick = { onClick() },
    )
}
