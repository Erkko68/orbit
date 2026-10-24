package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitSegmentedTabsTest : ComposeTest() {
    @Test
    fun selectsTheOptionTappedAsATab() = runComposeUiTest {
        var selected by mutableIntStateOf(0)
        setContent {
            OrbitTheme { OrbitSegmentedTabs(listOf("Items", "Agenda"), selected, { selected = it }) }
        }
        onNodeWithText("Items").assertIsSelected()
        onNodeWithText("Agenda").assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertTouchHeightIsEqualTo(48.dp)

        onNodeWithText("Agenda").performClick()

        assertEquals(1, selected)
        onNodeWithText("Agenda").assertIsSelected()
        onNodeWithText("Items").assertIsNotSelected()
    }
}
