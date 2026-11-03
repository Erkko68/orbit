package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.designsystem.ComposeTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitFloatingFacesTest : ComposeTest() {
    private fun face(name: String) = OrbitFloatingFace(name, name.take(1), Color.Blue, OrbitShape.Cookie, name)

    @Test
    fun aFaceSaysWhoItIsWhenTapped() = runComposeUiTest {
        var tapped = ""
        setContent {
            OrbitTheme {
                OrbitFloatingFaces(listOf(face("Ana"), face("Mika")), Modifier.size(200.dp), onFaceClick = { tapped = it })
            }
        }

        onNodeWithContentDescription("Mika").performClick()

        assertEquals("Mika", tapped)
    }

    @Test
    fun aBigFaceSitsInTheMiddleOfWhereItIsTapped() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            OrbitTheme {
                OrbitFloatingFaces(
                    faces = listOf(face("Ana")),
                    modifier = Modifier.size(300.dp),
                    largest = OrbitFacePoolLargest,
                    onFaceClick = {},
                )
            }
        }
        mainClock.advanceTimeBy(1_000)

        val touch = onNodeWithContentDescription("Ana").getUnclippedBoundsInRoot()
        val initial = onNodeWithText("A", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // The avatar is scaled up to the face, and a node's bounds keep its size from before that.
        val scale = touch.width / OrbitAvatarSize.Hero.size
        assertTrue(scale > 1f, "The face has to be bigger than the avatar it scales")
        val faceCenterX = (touch.left + touch.right) / 2
        val faceCenterY = (touch.top + touch.bottom) / 2
        assertTrue(abs((initial.left + initial.width * scale / 2 - faceCenterX).value) < 2f)
        assertTrue(abs((initial.top + initial.height * scale / 2 - faceCenterY).value) < 2f)
    }

    @Test
    fun facesWithoutAnActionAreOnlyDecoration() = runComposeUiTest {
        setContent { OrbitTheme { OrbitFloatingFaces(listOf(face("Ana")), Modifier.size(200.dp)) } }

        onNodeWithContentDescription("Ana").assertDoesNotExist()
    }
}
