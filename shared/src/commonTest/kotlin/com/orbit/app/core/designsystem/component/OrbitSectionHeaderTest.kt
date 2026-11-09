package com.orbit.app.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.test.performSemanticsAction

@OptIn(ExperimentalTestApi::class)
class OrbitSectionHeaderTest : ComposeTest() {
    @Test
    fun isAHeadingWithItsCount() = runComposeUiTest {
        setContent { OrbitTheme { OrbitSectionHeader("Active chores", count = "3 active") } }
        onNodeWithText("Active chores").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("3 active").assertIsDisplayed()
    }

    @Test
    fun showsNoCountWhenThereIsNone() = runComposeUiTest {
        setContent { OrbitTheme { OrbitSectionHeader("Payments") } }
        onNodeWithText("Payments").assertIsDisplayed()
    }

    @Test
    fun centresTheHeadingWhenRequestedWithoutChangingItsHeadingSemantics() = runComposeUiTest {
        setContent { OrbitTheme { OrbitSectionHeader("Balances", centered = true) } }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Balances")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(TextAlign.Center, layouts.single().layoutInput.style.textAlign)
    }
}
