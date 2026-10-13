package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitQuestionCardTest : ComposeTest() {
    private val people = listOf("Just me", "Two", "Three or more")

    @Test
    fun showsThePromptAndEveryOptionAndMarksTheSelectedOnes() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitQuestionCard("Who lives in the flat?", people, selected = setOf("Two"), onSelect = {})
            }
        }
        onNodeWithText("Who lives in the flat?").assertIsDisplayed()
        onNodeWithText("Just me").assertIsNotSelected()
        onNodeWithText("Two").assertIsSelected()
        onNodeWithText("Three or more").assertIsNotSelected()
    }

    @Test
    fun fillsTheWidthItIsGiven() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitQuestionCard(
                    prompt = "Any pets?",
                    options = listOf("Yes"),
                    selected = emptySet(),
                    onSelect = {},
                    modifier = Modifier.width(300.dp).testTag("card"),
                )
            }
        }
        onNodeWithTag("card").assertWidthIsEqualTo(300.dp)
    }

    @Test
    fun letsTheCallerPickOneAnswer() = runComposeUiTest {
        var selected by mutableStateOf(setOf("Just me"))
        setContent {
            OrbitTheme {
                OrbitQuestionCard("Who lives in the flat?", people, selected, onSelect = { selected = setOf(it) })
            }
        }
        onNodeWithText("Two").performClick()

        onNodeWithText("Two").assertIsSelected()
        onNodeWithText("Just me").assertIsNotSelected()
    }

    @Test
    fun letsTheCallerPickSeveralAnswers() = runComposeUiTest {
        var selected by mutableStateOf(emptySet<String>())
        setContent {
            OrbitTheme {
                OrbitQuestionCard(
                    prompt = "Which rooms need cleaning?",
                    options = listOf("Kitchen", "Bathroom"),
                    selected = selected,
                    onSelect = { selected = if (it in selected) selected - it else selected + it },
                )
            }
        }
        onNodeWithText("Kitchen").performClick()
        onNodeWithText("Bathroom").performClick()
        onNodeWithText("Kitchen").assertIsSelected()
        onNodeWithText("Bathroom").assertIsSelected()

        onNodeWithText("Kitchen").performClick()
        onNodeWithText("Kitchen").assertIsNotSelected()
        onNodeWithText("Bathroom").assertIsSelected()
    }

    @Test
    fun reportsTheTappedOption() = runComposeUiTest {
        val tapped = mutableListOf<String>()
        setContent {
            OrbitTheme {
                OrbitQuestionCard("Who lives in the flat?", people, selected = emptySet(), onSelect = { tapped += it })
            }
        }
        onNodeWithText("Three or more").performClick()
        assertEquals(listOf("Three or more"), tapped)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var prompt by mutableStateOf("Who lives in the flat?")
        var options by mutableStateOf(people)
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitQuestionCard(
                    prompt = prompt,
                    options = options,
                    selected = setOf("Two"),
                    onSelect = {},
                    modifier = Modifier.testTag("card"),
                )
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()
        onNodeWithTag("card").assertIsDisplayed()

        prompt = "Any pets?"
        options = listOf("Yes", "No")
        onNodeWithText("Any pets?").assertIsDisplayed()
        onNodeWithText("Yes").assertIsNotSelected()
    }
}
