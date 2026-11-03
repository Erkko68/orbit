package com.orbit.app.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitCrewTest : ComposeTest() {
    private fun face(id: String, name: String) =
        OrbitFloatingFace(id, name.take(1), Color.Magenta, OrbitShape.Circle, description = name)

    @Test
    fun everyFaceAndTheSettingsOpenTheSettings() = runComposeUiTest {
        var opened = 0
        setContent {
            OrbitTheme {
                OrbitCrew(
                    faces = listOf(face("ana", "Ana"), face("ben", "Ben")),
                    settingsDescription = "Space settings",
                    onClick = { opened++ },
                )
            }
        }

        onNodeWithContentDescription("Space settings").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Ana").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Ben").performClick()

        assertEquals(3, opened)
    }
}
