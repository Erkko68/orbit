package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitInsetTest : ComposeTest() {
    @Test
    fun showsItsRowInsideTonalAndElevatedCards() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Column {
                    OrbitCard { OrbitInset { Text("Oat milk") } }
                    OrbitCard(elevated = true) { OrbitInset { Text("Assigned to Elena") } }
                }
            }
        }
        onNodeWithText("Oat milk").assertIsDisplayed().assertHasNoClickAction()
        onNodeWithText("Assigned to Elena").assertIsDisplayed()
    }

    @Test
    fun clicksWhenItHasAnAction() = runComposeUiTest {
        var clicks = 0
        setContent { OrbitTheme { OrbitInset(onClick = { clicks++ }) { Text("Sourdough bread") } } }
        onNodeWithText("Sourdough bread").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }
}
