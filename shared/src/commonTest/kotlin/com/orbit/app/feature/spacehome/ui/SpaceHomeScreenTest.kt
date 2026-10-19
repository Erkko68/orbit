package com.orbit.app.feature.spacehome.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.domain.model.Preset
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    private val viewModel = SpaceHomeViewModel("space-1", spaces, items, InMemoryLastSpaceRepository())

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
                SpaceHomeScreen("space-1", onSwitchSpace = {}, onOpenItem = {}, viewModel = viewModel)
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
    fun theItemsTabKeepsItsFilterAcrossTabs() = runComposeUiTest {
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = {}, onOpenItem = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Lists").performScrollTo().performClick().assertIsSelected()

        onNodeWithText("Members").performClick()
        onNodeWithText("Items").performClick()

        onNodeWithText("Lists").performScrollTo().assertIsSelected()
    }

    @Test
    fun hasTheTopBarActions() = runComposeUiTest {
        var switches = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = { switches++ }, onOpenItem = {}, viewModel = viewModel)
            }
        }
        onNodeWithContentDescription("Switch space").performClick()
        assertEquals(1, switches)

        onNodeWithContentDescription("Notifications").assertIsNotEnabled()
    }

    @Test
    fun createsAnItemFromAPresetAndOpensIt() = runComposeUiTest {
        runBlocking { auth.register("ana@orbit.app", "secret", displayName = "Ana López") }
        val opened = mutableListOf<String>()
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = {}, onOpenItem = { opened += it }, viewModel = viewModel)
            }
        }
        onNodeWithText("New item").performClick()
        onNodeWithText("Create item").assertIsNotEnabled()

        onNodeWithText("List").performClick().assertIsSelected()
        onNodeWithText("Name it").performTextInput("Groceries")
        onNodeWithText("Create item").performClick()

        val created = runBlocking { items.observeItems("space-1").first().single() }
        assertEquals(Preset.SHOPPING_LIST, created.preset)
        waitForIdle()
        assertEquals(listOf(created.id), opened)
        onNodeWithText("Create item").assertDoesNotExist()
    }
}
