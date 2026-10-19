package com.orbit.app.feature.spacehome.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemoryLastSpaceRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

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
    fun showsFourAvatarsThenACountAndListsEveryone() = runComposeUiTest {
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
        val viewModel = SpaceHomeViewModel("space-1", spaces, items, InMemoryLastSpaceRepository())
        setContent {
            OrbitTheme {
                SpaceHomeScreen("space-1", onSwitchSpace = {}, onOpenItem = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("+2").assertIsDisplayed()

        onNodeWithText("Members").performClick()
        onNodeWithText("Flo").performScrollTo().assertIsDisplayed()
        onNodeWithText("Admin").performScrollTo().assertIsDisplayed()
        onAllNodesWithText("Member").assertCountEquals(4)
    }

    @Test
    fun listsTheItemsAndFiltersThemWithTheChips() = runComposeUiTest {
        auth.signIn("ana@orbit.app", "secret")
        val spaces = InMemorySpaceRepository(auth)
        val items = InMemoryItemRepository(auth)
        val id = spaces.createSpace("Flat 4B", accent = 0)
        items.create(id, NewItem("Book plumber", preset = Preset.TASK))
        items.create(id, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        startTestKoin(auth, spaces, items)
        val viewModel = SpaceHomeViewModel(id, spaces, items, InMemoryLastSpaceRepository())
        setContent {
            OrbitTheme {
                SpaceHomeScreen(id, onSwitchSpace = {}, onOpenItem = {}, viewModel = viewModel)
            }
        }
        onNodeWithText("Book plumber").performScrollTo().assertIsDisplayed()
        onNodeWithText("Groceries").performScrollTo().assertIsDisplayed()

        onNodeWithText("Lists").performScrollTo().performClick()
        onNodeWithText("Groceries").performScrollTo().assertIsDisplayed()
        onNodeWithText("Book plumber").assertDoesNotExist()
    }
}
