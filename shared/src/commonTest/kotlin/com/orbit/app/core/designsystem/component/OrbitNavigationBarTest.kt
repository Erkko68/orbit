package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_task
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitNavigationBarTest : ComposeTest() {
    @Test
    fun selectsTheDestinationTapped() = runComposeUiTest {
        var selected by mutableIntStateOf(0)
        val destinations = listOf(
            OrbitNavigationDestination("Home", Res.drawable.ic_home),
            OrbitNavigationDestination("Tasks", Res.drawable.ic_task),
        )
        setContent {
            OrbitTheme { OrbitNavigationBar(destinations, selected, { selected = it }) }
        }
        onNodeWithText("Home").assertIsSelected()
        onNodeWithText("Tasks").assertIsNotSelected()

        onNodeWithText("Tasks").performClick()

        assertEquals(1, selected)
        onNodeWithText("Tasks").assertIsSelected()
        onNodeWithText("Home").assertIsNotSelected()
    }
}
