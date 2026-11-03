package com.orbit.app.feature.spacehome.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsTab
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import org.robolectric.shadows.ShadowLooper
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/** Android only: the data arrives through the view model's flows, which these host tests run on the main looper. */
@OptIn(ExperimentalTestApi::class)
class SpaceHomeWithDataTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    // The items and members tabs get their view models from Koin.
    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun showsTheFacesOfTheMembersAndTheRestInTheSettings() = runComposeUiTest {
        auth.signIn("Ana", "secret")
        val spaces = FakeSpaceRepository.withMembers(
            "Ana" to Role.OWNER,
            "Ben" to Role.ADMIN,
            "Cy" to Role.MEMBER,
            "Di" to Role.MEMBER,
            "Ed" to Role.MEMBER,
            "Flo" to Role.MEMBER,
        )
        val items = InMemoryItemRepository(auth)
        startTestKoin(auth, spaces, items)
        val viewModel = SpaceHomeViewModel("space-1", spaces, items, InMemoryLastSpaceRepository(), auth)
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    "space-1",
                    onBack = {},
                    onOpenAccount = {},
                    onOpenItem = {},
                    onNewItem = {},
                    onOpenAssistant = {},
                    viewModel = viewModel,
                )
            }
        }
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onAllNodesWithText("A")[0].assertIsDisplayed()

        onNodeWithContentDescription("Space settings").performClick()
        onNodeWithText("6 members").assertIsDisplayed()
        onNodeWithText("Members and roles").performClick()
        onNodeWithText("You").assertExists()
        onNodeWithText("Owner").assertExists()
        onNodeWithText("Admins").assertExists()
    }

    @Test
    fun listsTheItemsAndFiltersThemWithTheChips() = runComposeUiTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        val items = InMemoryItemRepository(auth)
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber"))
        val groceriesId = items.create(id, NewItem("Groceries", modules = ItemModules(checklist = Checklist(emptyList()))))
        val opened = mutableListOf<String>()
        startTestKoin(auth, spaces, items)
        val viewModel = SpaceHomeViewModel(id, spaces, items, InMemoryLastSpaceRepository(), auth)
        setContent {
            OrbitTheme {
                SpaceHomeScreen(
                    id,
                    onBack = {},
                    onOpenAccount = {},
                    onOpenItem = { opened += it },
                    onNewItem = {},
                    onOpenAssistant = {},
                    viewModel = viewModel,
                )
            }
        }
        onAllNodesWithText("Tasks")[0].performClick()
        onNodeWithText("Book plumber").assertExists()
        onNodeWithText("Groceries").assertExists()

        onNode(hasText("Lists") and hasClickAction()).performClick()
        onNodeWithText("Groceries").performScrollTo().assertIsDisplayed()
        onNodeWithText("Book plumber").assertDoesNotExist()
        onNodeWithText("Groceries").performScrollTo().performClick()
        assertEquals(listOf(groceriesId), opened)
    }

    @Test
    fun completesAndReopensAnItemFromTheList() = runComposeUiTest {
        auth.signIn("ana@orbit.app", "secret")
        val space = Space("space-2", "Flat 4B", accent = 0, memberIds = listOf("ana@orbit.app"))
        val spaces = FakeSpaceRepository(space, listOf(Member("ana@orbit.app", "Ana", null, Role.OWNER)))
        val items = InMemoryItemRepository(auth)
        val id = "space-2"
        val itemId = items.create(id, NewItem("Buy bread"))
        startTestKoin(auth, spaces, items)
        val opened = mutableListOf<String>()
        setContent {
            OrbitTheme {
                OrbitScreen(scrollable = true) {
                    SpaceItemsTab(id, onOpenItem = { opened += it })
                }
            }
        }

        onNodeWithText("Buy bread").assertIsDisplayed()
        onNodeWithContentDescription("Complete Buy bread").performScrollTo().performClick()
        ShadowLooper.idleMainLooper()
        waitForIdle()

        onNodeWithContentDescription("Reopen Buy bread").assertIsDisplayed()
        val completed = runBlocking { items.observeItems(id).first().single { it.id == itemId } }
        assertTrue(completed.status is ItemStatus.Done, "Expected item to be Done but was: ${completed.status}")

        onNodeWithContentDescription("Reopen Buy bread").performScrollTo().performClick()
        ShadowLooper.idleMainLooper()
        waitForIdle()

        onNodeWithContentDescription("Complete Buy bread").assertIsDisplayed()
        assertTrue(opened.isEmpty(), "Toggling completion must not open the detail")
        val reopened = runBlocking { items.observeItems(id).first().single { it.id == itemId } }
        assertTrue(reopened.status is ItemStatus.Open, "Expected item to be Open but was: ${reopened.status}")
    }
}
