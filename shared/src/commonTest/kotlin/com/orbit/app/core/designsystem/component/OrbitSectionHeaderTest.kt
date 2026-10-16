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
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_task
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitSectionHeaderTest : ComposeTest() {
    @Test
    fun isAHeadingWithItsCount() = runComposeUiTest {
        setContent { OrbitTheme { OrbitSectionHeader("Active chores", Res.drawable.ic_task, count = "3 active") } }
        onNodeWithText("Active chores").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        onNodeWithText("3 active").assertIsDisplayed()
    }

    @Test
    fun showsNoCountWhenThereIsNone() = runComposeUiTest {
        setContent { OrbitTheme { OrbitSectionHeader("Payments", Res.drawable.ic_task) } }
        onNodeWithText("Payments").assertIsDisplayed()
    }
}
