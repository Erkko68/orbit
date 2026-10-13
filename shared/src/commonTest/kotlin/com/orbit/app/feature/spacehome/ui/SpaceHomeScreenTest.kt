package com.orbit.app.feature.spacehome.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.testing.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpaceHomeScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val viewModel = SpaceHomeViewModel("space-1", spaces, items)

    // The items and members tabs get their view models from Koin.
    @BeforeTest
    fun startDi() {
        startTestKoin(auth, spaces, items)
    }

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun showsTheSummaryAndSwitchesBetweenTabs() = runComposeUiTest {
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("open tasks").assertIsDisplayed()
        onNodeWithText("bills due").assertIsDisplayed()
        onNodeWithText("events today").assertIsDisplayed()

        onNodeWithText("Items").assertIsSelected()
        onNodeWithText("All").assertIsSelected()
        onNodeWithText("Tasks").assertIsNotSelected()
        onNodeWithText("No items yet").assertIsDisplayed()

        onNodeWithText("Agenda").performClick().assertIsSelected()
        onNodeWithText("Today").assertIsDisplayed()
        onNodeWithText("Nothing scheduled for today").assertIsDisplayed()

        onNodeWithText("Members").performClick().assertIsSelected()
        onNodeWithText("Nothing scheduled for today").assertDoesNotExist()
    }

    @Test
    fun hasTheTopBarActionsAndTheNewItemPlaceholder() = runComposeUiTest {
        var switches = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = { switches++ }, viewModel = viewModel)
            }
        }
        onNodeWithContentDescription("Switch space").performClick()
        assertEquals(1, switches)

        onNodeWithContentDescription("Notifications").assertIsNotEnabled()
        onNodeWithText("New item").assertIsDisplayed()
    }
}
