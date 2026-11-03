package com.orbit.app.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.hasClickLabel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitCrewTest : ComposeTest() {
    private fun face(id: String, name: String) =
        OrbitFloatingFace(id, name.take(1), Color.Magenta, OrbitShape.Circle, description = name)

    @Test
    fun everyFaceOpensTheSettings() = runComposeUiTest {
        var opened = 0
        setContent {
            OrbitTheme {
                OrbitCrew(
                    faces = listOf(face("ana", "Ana"), face("ben", "Ben")),
                    openLabel = "Space settings",
                    onClick = { opened++ },
                )
            }
        }

        onNodeWithContentDescription("Ana").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Ben").assertIsDisplayed().performClick()

        assertEquals(2, opened)
        onAllNodes(hasClickLabel("Space settings")).assertCountEquals(2)
    }
}
