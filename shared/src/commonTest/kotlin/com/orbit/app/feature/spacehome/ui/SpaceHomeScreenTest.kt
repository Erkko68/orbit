package com.orbit.app.feature.spacehome.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.hasClickLabel
import com.orbit.app.testing.startTestKoin
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
    private val viewModel = SpaceHomeViewModel("space-1", spaces, items, InMemoryLastSpaceRepository(), auth)
    private var settingsOpened = 0

    // The items get their view models from Koin.
    @BeforeTest
    fun startDi() {
        startTestKoin(auth, spaces, items)
    }

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    private fun ComposeUiTest.showTheSpace() = setContent {
        OrbitTheme {
            SpaceHomeScreen("space-1", onBack = {}, onOpenAccount = {}, onOpenItem = {}, onNewItem = {}, onOpenAssistant = {}, onOpenSettings = { settingsOpened++ }, viewModel = viewModel)
        }
    }

    @Test
    fun opensOnCalendarWithItsHeadlineAndTheAgenda() = runComposeUiTest {
        runBlocking {
            auth.register("ana@orbit.app", "secret", displayName = "Ana López")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        showTheSpace()

        onNodeWithText("Calendar").assertIsSelected()
        onNodeWithText("Nothing in orbit yet").assertIsDisplayed()
        onNodeWithText("Nothing scheduled for today").assertIsDisplayed()
        onAllNodes(hasClickLabel("Space settings")).onFirst().assertIsDisplayed()
    }

    @Test
    fun theFacesAndTheSettingsButtonOpenTheSettings() = runComposeUiTest {
        runBlocking {
            auth.register("ana@orbit.app", "secret", displayName = "Ana López")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        showTheSpace()

        onNode(hasClickLabel("Space settings")).performClick()
        onNodeWithContentDescription("Space settings").performClick()

        assertEquals(2, settingsOpened)
    }

    @Test
    fun theBarSwitchesBetweenTheDestinations() = runComposeUiTest {
        runBlocking {
            auth.register("ana@orbit.app", "secret", displayName = "Ana López")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        showTheSpace()
        onNodeWithContentDescription("Add").assertIsDisplayed()

        onAllNodesWithText("Tasks")[0].performClick()
        onNodeWithText("No items yet").assertIsDisplayed()
        onNodeWithContentDescription("Add").assertIsDisplayed()

        onNode(hasText("Expenses") and hasClickAction()).performClick().assertIsSelected()
        onNodeWithText("No expenses yet").assertIsDisplayed()
        onNodeWithContentDescription("Add").assertDoesNotExist()

        onNodeWithText("Calendar").performClick().assertIsSelected()
        onNodeWithText("No expenses yet").assertDoesNotExist()
    }

    @Test
    fun theItemsKeepTheirFilterAcrossDestinations() = runComposeUiTest {
        showTheSpace()
        onAllNodesWithText("Tasks")[0].performClick()
        onNodeWithText("Lists").performScrollTo().performClick().assertIsSelected()

        onNodeWithText("Expenses").performClick()
        onAllNodesWithText("Tasks")[0].performClick()

        onNodeWithText("Lists").performScrollTo().assertIsSelected()
    }

    @Test
    fun hasTheTopBarActions() = runComposeUiTest {
        var backs = 0
        var accounts = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    "space-1",
                    onBack = { backs++ },
                    onOpenAccount = { accounts++ },
                    onOpenItem = {},
                    onNewItem = {},
                    onOpenAssistant = {},
                    onOpenSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        onNodeWithContentDescription("Account").performClick()
        assertEquals(1, accounts)
        assertEquals(0, backs)
    }

    @Test
    fun theTitleOpensTheSpaces() = runComposeUiTest {
        runBlocking {
            auth.register("ana@orbit.app", "secret", displayName = "Ana López")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        var backs = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    "space-1",
                    onBack = { backs++ },
                    onOpenAccount = {},
                    onOpenItem = {},
                    onNewItem = {},
                    onOpenAssistant = {},
                    onOpenSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        onNodeWithText("Flat 4B").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun showsTheFacesOfTheMembers() = runComposeUiTest {
        runBlocking {
            auth.register("ana@orbit.app", "secret", displayName = "Ana López")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        showTheSpace()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("1 member").assertDoesNotExist()
        onAllNodesWithText("AL").assertCountEquals(2)
    }

    @Test
    fun theAddButtonOpensANewItem() = runComposeUiTest {
        var added = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    "space-1",
                    onBack = {},
                    onOpenAccount = {},
                    onOpenItem = {},
                    onNewItem = { added++ },
                    onOpenAssistant = {},
                    onOpenSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        onNodeWithContentDescription("Add").performClick()

        assertEquals(1, added)
    }

    @Test
    fun theCalendarOpensTheAssistant() = runComposeUiTest {
        var opened = 0
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    "space-1",
                    onBack = {},
                    onOpenAccount = {},
                    onOpenItem = {},
                    onNewItem = {},
                    onOpenAssistant = { opened++ },
                    onOpenSettings = {},
                    viewModel = viewModel,
                )
            }
        }
        onNodeWithText("Plan with the assistant").performScrollTo().performClick()

        assertEquals(1, opened)
    }
}
