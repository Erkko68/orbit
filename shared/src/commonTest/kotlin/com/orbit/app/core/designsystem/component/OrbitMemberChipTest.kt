package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitMemberChipTest : ComposeTest() {
    @Test
    fun showsTheNameAndTheDetailAndActsWhenTapped() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitMemberChip(
                    name = "Ana",
                    detail = "Owner",
                    leading = { OrbitAvatar("A", OrbitTheme.colors.spaceAccent(0)) },
                    onClick = { clicks++ },
                )
            }
        }
        onNodeWithText("Owner").assertIsDisplayed()
        onNodeWithText("Ana").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun isNotTappableWithoutAnAction() = runComposeUiTest {
        setContent {
            OrbitTheme { OrbitMemberChip("Ben", leading = {}, highlighted = true) }
        }
        onNodeWithText("Ben").assertHasNoClickAction()
    }
}
