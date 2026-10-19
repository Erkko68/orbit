package com.orbit.app.core.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryLastSpaceRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.di.featureModule
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.LastSpaceRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The whole shell through Koin, as the app runs it. Android only: it drives a real NavHost, which
 * needs the activity's back dispatcher and view model store.
 */
@OptIn(ExperimentalTestApi::class)
class OrbitNavHostTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val lastSpace = InMemoryLastSpaceRepository()

    @BeforeTest
    fun startDi() {
        startKoin {
            modules(
                module {
                    single<AuthRepository> { auth }
                    single<SpaceRepository> { spaces }
                    single<ItemRepository> { items }
                    single<LastSpaceRepository> { lastSpace }
                    single<InvitationRepository> { InMemoryInvitationRepository(auth) }
                    single<TextSharer> { TextSharer {} }
                },
                featureModule,
            )
        }
    }

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun signsInCreatesASpaceAndSwitchesBack() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitNavHost()
            }
        }
        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("No spaces yet").assertIsDisplayed()
        onNodeWithText("New space").performClick()
        onNode(hasSetTextAction() and isEnabled()).performTextInput("Flat 4B")
        onNodeWithText("Create space").performClick()
        onNodeWithText("1 member").assertIsDisplayed()
        onNodeWithText("Flat 4B").performScrollTo().performClick()

        onNodeWithText("No items yet").assertIsDisplayed()
        onNodeWithText("Members").performClick()
        onNodeWithText("ana").performScrollTo().assertIsDisplayed()
        onNodeWithText("Owner").performScrollTo().assertIsDisplayed()

        onNodeWithContentDescription("Switch space").performClick()
        onNodeWithText("New space").assertIsDisplayed()
    }

    @Test
    fun createsAnAccountFromSignInAndLandsInTheSpaces() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitNavHost()
            }
        }
        onNodeWithText("Create account").performScrollTo().performClick()
        onNodeWithText("Create your account").assertIsDisplayed()

        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("12345678")
        onNodeWithText("Confirm password").performTextInput("12345678")
        onNodeWithText("Create account").performScrollTo().performClick()

        onNodeWithText("No spaces yet").assertIsDisplayed()
        onNodeWithText("Create your account").assertDoesNotExist()
    }

    @Test
    fun goesBackToSignInFromCreateAccount() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitNavHost()
            }
        }
        onNodeWithText("Create account").performScrollTo().performClick()
        onNodeWithText("Create your account").assertIsDisplayed()

        onNodeWithText("Sign in").performScrollTo().performClick()

        // Sign-in comes back scrolled where it was left, down at "Create account".
        onNodeWithText("Welcome back").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signingOutGoesBackToSignIn() = runComposeUiTest {
        runBlocking { auth.signIn("ana@orbit.app", "secret") }
        setContent {
            OrbitTheme {
                OrbitNavHost()
            }
        }
        onNodeWithText("New space").assertIsDisplayed()

        auth.signOut()

        onNodeWithText("Welcome back").assertIsDisplayed()
        onNodeWithText("New space").assertDoesNotExist()
    }

    @Test
    fun opensAnItemThroughKoinChangesItAndReturnsToTheSpace() = runComposeUiTest {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            val spaceId = spaces.createSpace("Flat 4B", accent = 0)
            items.create(
                spaceId, NewItem(
                    "Book plumber", preset = Preset.TASK,
                    modules = ItemModules(checklist = Checklist(emptyList()))
                )
            )
            items.create(spaceId, NewItem("Groceries", preset = Preset.SHOPPING_LIST))
        }
        setContent { OrbitTheme { OrbitNavHost() } }
        onNodeWithText("Flat 4B").performScrollTo().performClick()
        onNodeWithText("Book plumber").performScrollTo().performClick()
        onNodeWithContentDescription("Remove checklist").performScrollTo().performClick()
        onNodeWithText("+ Checklist").performScrollTo().assertIsDisplayed()
        // The top bar is outside the scroll container and always remains visible.
        onNodeWithContentDescription("Back to space").performClick()
        onNodeWithText("Items").assertIsDisplayed()
        onNodeWithText("Groceries").performScrollTo().performClick()
        onNodeWithText("+ Checklist").performScrollTo().performClick()
        onNodeWithText("Checklist").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Back to space").performClick()
        onNodeWithContentDescription("Switch space").assertIsDisplayed()
    }

    @Test
    fun opensIntoTheLastSpaceAndSwitchesToTheList() = runComposeUiTest {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            lastSpace.remember(spaces.createSpace("Flat 4B", accent = 0))
        }
        setContent { OrbitTheme { OrbitNavHost() } }

        onNodeWithText("No items yet").assertIsDisplayed()
        onNodeWithContentDescription("Switch space").performClick()

        onNodeWithText("New space").assertIsDisplayed()
        onNodeWithText("No items yet").assertDoesNotExist()
    }

    @Test
    fun opensTheListWhenTheLastSpaceIsNoLongerTheUsers() = runComposeUiTest {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        lastSpace.remember("a-space-ana-left")
        setContent { OrbitTheme { OrbitNavHost() } }

        onNodeWithText("New space").assertIsDisplayed()
        onNodeWithText("No items yet").assertDoesNotExist()
        assertNull(lastSpace.lastSpaceId())
    }

    @Test
    fun remembersTheSpaceTheUserOpens() = runComposeUiTest {
        val spaceId = runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            spaces.createSpace("Flat 4B", accent = 0)
        }
        setContent { OrbitTheme { OrbitNavHost() } }

        onNodeWithText("Flat 4B").performScrollTo().performClick()

        onNodeWithText("No items yet").assertIsDisplayed()
        assertEquals(spaceId, lastSpace.lastSpaceId())
    }
}
