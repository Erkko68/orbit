package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitColorPickerTest : ComposeTest() {
    private val colors = listOf(Color.Red, Color.Green, Color.Blue)
    private val labels = listOf("Red", "Green", "Blue")

    @Test
    fun showsEachColourNamedForScreenReaders() = runComposeUiTest {
        setContent {
            OrbitTheme { OrbitColorPicker(colors, labels, selectedIndex = 0, onSelect = {}) }
        }

        labels.forEach { onNodeWithContentDescription(it).assertIsDisplayed() }
    }

    @Test
    fun ringsTheSelectedColourAndSelectsTheOneTapped() = runComposeUiTest {
        var selected by mutableStateOf(1)
        val picked = mutableListOf<Int>()
        setContent {
            OrbitTheme {
                OrbitColorPicker(colors, labels, selectedIndex = selected, onSelect = { picked += it; selected = it })
            }
        }
        onNodeWithContentDescription("Green").assertIsSelected()
        onNodeWithContentDescription("Red").assertIsNotSelected()

        onNodeWithContentDescription("Blue").performClick()

        assertEquals(listOf(2), picked)
        onNodeWithContentDescription("Blue").assertIsSelected()
        onNodeWithContentDescription("Green").assertIsNotSelected()
    }

    @Test
    fun hasA48dpTouchTargetForEachColour() = runComposeUiTest {
        setContent {
            OrbitTheme { OrbitColorPicker(colors, labels, selectedIndex = 0, onSelect = {}) }
        }

        labels.forEach {
            onNodeWithContentDescription(it).assertTouchWidthIsEqualTo(48.dp).assertTouchHeightIsEqualTo(48.dp)
        }
    }
}
