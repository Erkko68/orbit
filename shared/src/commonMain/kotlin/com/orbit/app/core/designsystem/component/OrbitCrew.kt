package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CrewStage = 64.dp
private val CrewFace = 20.dp

/** More would not fit the top bar and still turn: the rest are in the space's settings, which the faces open. */
private const val CREW_FACES = 4

/**
 * The people of a space orbiting in the top bar, the first few of them. They are the way into the space's settings:
 * tapping any of them calls [onClick], which [openLabel] names for screen readers.
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
        modifier = modifier.size(CrewStage),
        largest = CrewFace,
        limit = CREW_FACES,
        onFaceClick = { onClick() },
        clickLabel = openLabel,
    )
}
