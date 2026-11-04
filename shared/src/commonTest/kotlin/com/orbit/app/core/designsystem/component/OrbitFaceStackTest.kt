package com.orbit.app.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.hasClickLabel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitFaceStackTest : ComposeTest() {
    private fun face(name: String) = OrbitFace(name, name, name.take(1), Color.Cyan, OrbitShape.Cookie)

    @Test
    fun showsFourFacesAndCountsTheRest() = runComposeUiTest {
        setContent { OrbitTheme { OrbitFaceStack(listOf("Ana", "Ben", "Cy", "Di", "Ed", "Flo").map(::face)) } }

        onNodeWithContentDescription("Di", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("Ed", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("+2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun fourOrFewerHaveNoCount() = runComposeUiTest {
        setContent { OrbitTheme { OrbitFaceStack(listOf("Ana", "Ben").map(::face)) } }

        onNodeWithText("+", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun withAClickTheStackIsOneButton() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme { OrbitFaceStack(listOf(face("Ana")), onClick = { clicks++ }, clickLabel = "Space settings") }
        }

        onNode(hasClickLabel("Space settings")).performClick()

        assertEquals(1, clicks)
    }
}
