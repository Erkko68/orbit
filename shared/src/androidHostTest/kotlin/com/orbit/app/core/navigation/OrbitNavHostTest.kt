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
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.di.featureModule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The whole shell through Koin, as the app runs it. Android only: it drives a real NavHost, which
 * needs the activity's back dispatcher and view model store.
 */
@OptIn(ExperimentalTestApi::class)
class OrbitNavHostTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    @BeforeTest
    fun startDi() {
        startKoin {
            modules(
                module {
                    single<AuthRepository> { auth }
                    single<SpaceRepository> { InMemorySpaceRepository(auth) }
                    single<ItemRepository> { InMemoryItemRepository(auth) }
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
    fun signingOutGoesBackToSignIn() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitNavHost()
            }
        }
        onNodeWithText("Sign in").performScrollTo().performClick()
        onNodeWithText("New space").assertIsDisplayed()

        auth.signOut()

        onNodeWithText("Welcome back").assertIsDisplayed()
        onNodeWithText("New space").assertDoesNotExist()
    }
}
