package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitListItemTest : ComposeTest() {
    @Test
    fun showsItsTextsBetweenItsLeadingAndTrailingContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitListItem("Flat 4B", "2 members", leading = { Text("F") }, trailing = { Text(">") })
            }
        }
        val leading = onNodeWithText("F").assertIsDisplayed().getUnclippedBoundsInRoot()
        val title = onNodeWithText("Flat 4B").assertIsDisplayed().getUnclippedBoundsInRoot()
        val trailing = onNodeWithText(">").assertIsDisplayed().getUnclippedBoundsInRoot()
        onNodeWithText("2 members").assertIsDisplayed()
        assertTrue(leading.right <= title.left)
        assertTrue(title.right <= trailing.left)
    }

    @Test
    fun worksWithoutLeadingOrTrailingContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitListItem("Ana", "Owner")
            }
        }
        onNodeWithText("Ana").assertIsDisplayed()
        onNodeWithText("Owner").assertIsDisplayed()
    }
}
