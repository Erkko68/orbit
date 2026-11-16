package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class OrbitDatePickerTest : ComposeTest() {
    @Test
    fun confirmsTheSelectedDay() = runComposeUiTest {
        val day = LocalDate(2026, 11, 4)
        var picked: LocalDate? = null
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { OrbitDatePicker(day, "Pick", { picked = it }) } } }
        onNodeWithText("Pick").performScrollTo().performClick()
        assertEquals(day, picked)
    }

    @Test
    fun withoutADayThereIsNothingToConfirm() = runComposeUiTest {
        var picked: LocalDate? = null
        setContent { OrbitTheme { OrbitDatePicker(null, "Pick", { picked = it }) } }
        onNodeWithText("Pick").assertIsNotEnabled()
        assertNull(picked)
    }
}
