package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitIconButtonTest : ComposeTest() {
    @Test
    fun isFoundByItsDescriptionAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitIconButton(Res.drawable.ic_add, "Add", onClick = { clicks++ })
            }
        }
        onNodeWithContentDescription("Add").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun canBeDisabled() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitIconButton(Res.drawable.ic_add, "Add", onClick = {}, enabled = false)
            }
        }
        onNodeWithContentDescription("Add").assertIsNotEnabled()
    }
}
