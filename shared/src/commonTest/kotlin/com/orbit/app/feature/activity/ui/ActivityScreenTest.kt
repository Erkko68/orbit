package com.orbit.app.feature.activity.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.feature.activity.ActivityViewModel
import com.orbit.app.testing.InMemoryActivityRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class ActivityScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private var backs = 0

    /** Ana owns Flat 4B and Ben is a member. Ana is signed in. */
    private fun ComposeUiTest.show(history: InMemoryActivityRepository) {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            spaces.createSpace("Flat 4B", accent = 0)
            spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.MEMBER))
        }
        setContent {
            OrbitTheme {
                ActivityScreen(
                    spaceId = "space-1",
                    onBack = { backs++ },
                    viewModel = ActivityViewModel("space-1", history, spaces, auth),
                )
            }
        }
    }

    private fun entry(id: String, by: String, action: ActivityAction, byAssistant: Boolean = false) = Activity(
        id, by, Clock.System.now() - 1.minutes, action, ActivityTarget.ITEM, "item-$id", id, byAssistant = byAssistant,
    )

    @Test
    fun saysWhoDidWhatUnderToday() = runComposeUiTest {
        val history = InMemoryActivityRepository()
        history.record("space-1", entry("Dinner", "ben@orbit.app", ActivityAction.COMPLETE))
        history.record("space-1", entry("Bins", "ana@orbit.app", ActivityAction.DELETE))
        show(history)

        onNodeWithText("Today").assertIsDisplayed()
        onNodeWithText("Ben completed Dinner").assertIsDisplayed()
        onNodeWithText("You deleted Bins").assertIsDisplayed()
    }

    @Test
    fun labelsWhatTheAssistantDid() = runComposeUiTest {
        val history = InMemoryActivityRepository()
        history.record("space-1", entry("Plants", "ana@orbit.app", ActivityAction.CREATE, byAssistant = true))
        show(history)

        onNodeWithText("You added Plants").assertIsDisplayed()
        onNodeWithText("with the assistant", substring = true).assertIsDisplayed()
    }

    @Test
    fun anEmptyHistorySaysSo() = runComposeUiTest {
        show(InMemoryActivityRepository())

        onNodeWithText("Nothing has changed yet").assertIsDisplayed()
    }

    @Test
    fun aFailedReadRetries() = runComposeUiTest {
        val history = InMemoryActivityRepository(failures = 1)
        history.record("space-1", entry("Dinner", "ben@orbit.app", ActivityAction.EDIT))
        show(history)

        onNodeWithText("Could not load the activity").assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()

        onNodeWithText("Ben edited Dinner").assertIsDisplayed()
    }

    @Test
    fun theBackArrowGoesBack() = runComposeUiTest {
        show(InMemoryActivityRepository())

        onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
