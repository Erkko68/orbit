package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_lock
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitListCardTest : ComposeTest() {
    @Test
    fun showsItsTextsAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitListCard(
                    title = "Change password",
                    supporting = "Use at least 8 characters.",
                    icon = Res.drawable.ic_lock,
                    onClick = { clicks++ },
                )
            }
        }
        onNodeWithText("Use at least 8 characters.", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Change password", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Change password").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun isNotTappableWithoutAnAction() = runComposeUiTest {
        setContent { OrbitTheme { OrbitListCard("Groceries", "List", Res.drawable.ic_lock) } }
        onNodeWithText("Groceries").assertIsDisplayed().assertHasNoClickAction()
    }
}
