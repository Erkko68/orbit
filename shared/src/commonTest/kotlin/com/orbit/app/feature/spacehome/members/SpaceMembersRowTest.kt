package com.orbit.app.feature.spacehome.members

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Role
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpaceMembersRowTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun showsAFaceForEachMemberAndTheSettings() = runComposeUiTest {
        runBlocking { auth.signIn("cy@orbit.app", "secret") }
        val spaces = FakeSpaceRepository.withMembers(
            "ana@orbit.app" to Role.OWNER,
            "ben@orbit.app" to Role.ADMIN,
            "cy@orbit.app" to Role.MEMBER,
        )
        startTestKoin(auth, spaces)
        setContent {
            OrbitTheme {
                SpaceMembersRow("space-1", onGone = {}, viewModel = SpaceMembersViewModel("space-1", spaces, auth))
            }
        }

        onNodeWithText("A").assertIsDisplayed()
        onNodeWithText("B").assertIsDisplayed()
        onNodeWithText("C").assertIsDisplayed()
        onNodeWithContentDescription("Space settings").assertIsDisplayed()
    }

    @Test
    fun saysWhenTheMembersFailToLoad() = runComposeUiTest {
        val spaces = FlakySpaceRepository(memberFailures = 1)
        startTestKoin(auth, spaces)
        setContent {
            OrbitTheme {
                SpaceMembersRow("space-1", onGone = {}, viewModel = SpaceMembersViewModel("space-1", spaces, auth))
            }
        }

        onNodeWithText("Could not load the members").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        onNodeWithText("Could not load the members").assertDoesNotExist()
    }
}
