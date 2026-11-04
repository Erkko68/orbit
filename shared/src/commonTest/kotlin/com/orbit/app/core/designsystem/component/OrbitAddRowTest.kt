package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAddRowTest : ComposeTest() {
    @Test
    fun theButtonAndTheKeyboardBothAdd() = runComposeUiTest {
        var added = 0
        var typed = ""
        setContent { OrbitTheme { OrbitAddRow("", { typed = it }, "Add to the list", { added++ }) } }
        val field = onNode(hasSetTextAction() and hasContentDescription("Add to the list"))

        field.performTextInput("Milk")
        field.performImeAction()
        onNode(hasClickAction() and !hasSetTextAction() and hasContentDescription("Add to the list")).performClick()

        assertEquals("Milk", typed)
        assertEquals(2, added)
    }

    @Test
    fun cannotAddWhenThereIsNothingToAdd() = runComposeUiTest {
        setContent { OrbitTheme { OrbitAddRow("", {}, "Add to the list", {}, canAdd = false) } }

        onNode(hasClickAction() and !hasSetTextAction() and hasContentDescription("Add to the list")).assertIsNotEnabled()
    }
}
