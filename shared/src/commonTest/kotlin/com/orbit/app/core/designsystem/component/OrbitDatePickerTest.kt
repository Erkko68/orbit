package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class OrbitDatePickerTest : ComposeTest() {
    @Test
    fun confirmsTheSelectedDayAsUtcMidnight() = runComposeUiTest {
        val day = LocalDate(2026, 11, 4).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        var picked: Long? = null
        setContent { OrbitTheme { OrbitScreen(scrollable = true) { OrbitDatePicker(day, "Pick", { picked = it }) } } }
        onNodeWithText("Pick").performScrollTo().performClick()
        assertEquals(day, picked)
    }

    @Test
    fun withoutADayThereIsNothingToConfirm() = runComposeUiTest {
        var picked: Long? = null
        setContent { OrbitTheme { OrbitDatePicker(null, "Pick", { picked = it }) } }
        onNodeWithText("Pick").assertIsNotEnabled()
        assertNull(picked)
    }
}
