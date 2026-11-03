package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CrewWidth = 132.dp
private val CrewHeight = 56.dp
private val CrewFace = 20.dp

/**
 * The people of a space orbiting in the top bar. They are the way into the space's settings: tapping any of them calls
 * [onClick], which [openLabel] names for screen readers.
 */
@Composable
fun OrbitCrew(
    faces: List<OrbitFloatingFace>,
    openLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitFloatingFaces(
        faces = faces,
        modifier = modifier.size(CrewWidth, CrewHeight),
        largest = CrewFace,
        onFaceClick = { onClick() },
        clickLabel = openLabel,
    )
}
