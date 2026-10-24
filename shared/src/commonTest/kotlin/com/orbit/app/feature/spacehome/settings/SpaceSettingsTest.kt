package com.orbit.app.feature.spacehome.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpaceSettingsTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private var gone = 0

    /** Ana owns Flat 4B, in the second colour (Coral). Ben is an admin of it and Cy a plain member. */
    private fun ComposeUiTest.showSettingsAs(email: String, repository: SpaceRepository = spaces) {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            spaces.createSpace("Flat 4B", accent = 1)
            spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.ADMIN))
            spaces.addMember("space-1", Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
            auth.signOut()
            auth.signIn(email, "secret")
        }
        setContent {
            OrbitTheme {
                SpaceSettings(
                    "space-1",
                    onGone = { gone++ },
                    viewModel = SpaceSettingsViewModel("space-1", repository, auth),
                )
            }
        }
    }

    private fun spaceNow() = runBlocking { spaces.observeSpaces().first().single() }

    @Test
    fun theOwnerAndAnAdminSeeTheButton() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").assertIsDisplayed()
    }

    @Test
    fun anAdminSeesTheButtonToo() = runComposeUiTest {
        showSettingsAs("ben@orbit.app")
        onNodeWithContentDescription("Space settings").assertIsDisplayed()
    }

    @Test
    fun aPlainMemberOnlySeesLeaveSpace() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")

        onNodeWithContentDescription("Space settings").assertDoesNotExist()
        onNodeWithContentDescription("Leave space").performClick()
        onNodeWithText("Leave this space?").assertIsDisplayed()
    }

    @Test
    fun aMemberLeavesAfterConfirming() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        onNodeWithContentDescription("Leave space").performClick()

        onNodeWithText("Leave").performClick()

        waitForIdle()
        assertEquals(1, gone)
        val members = runBlocking { spaces.observeMembers("space-1").first() }
        assertEquals(listOf("ana@orbit.app", "ben@orbit.app"), members.map { it.userId })
    }

    @Test
    fun anAdminLeavesFromTheSettings() = runComposeUiTest {
        showSettingsAs("ben@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()

        onNodeWithText("Leave space").performScrollTo().performClick()
        onNodeWithText("Leave").performClick()

        waitForIdle()
        assertEquals(1, gone)
    }

    @Test
    fun theOwnerDeletesTheSpaceAfterConfirming() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()
        onNodeWithText("Leave space").assertDoesNotExist()

        onNodeWithText("Delete space").performScrollTo().performClick()
        onNodeWithText("Delete this space?").assertIsDisplayed()
        onNodeWithText("Delete").performClick()

        waitForIdle()
        assertEquals(1, gone)
        assertEquals(emptyList(), runBlocking { spaces.observeSpaces().first() })
    }

    @Test
    fun cancellingTheDeleteGoesBackToTheSettings() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()
        onNodeWithText("Delete space").performScrollTo().performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Delete this space?").assertDoesNotExist()
        onNodeWithText("Space settings").assertIsDisplayed()
        assertEquals(0, gone)
    }

    @Test
    fun saysWhenLeavingFailed() = runComposeUiTest {
        val failing = object : SpaceRepository by spaces {
            override suspend fun leaveSpace(spaceId: String) = error("Could not reach the server")
        }
        showSettingsAs("cy@orbit.app", failing)
        onNodeWithContentDescription("Leave space").performClick()

        onNodeWithText("Leave").performClick()

        onNodeWithText("Could not leave the space. Try again.").assertIsDisplayed()
        assertEquals(0, gone)
    }

    @Test
    fun opensWithTheCurrentNameAndColourPicked() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNodeWithContentDescription("Space settings").performClick()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithContentDescription("Coral").assertIsSelected()
        onNodeWithContentDescription("Violet").assertIsNotSelected()
        onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun savesTheNewNameAndColour() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()

        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")
        onNodeWithContentDescription("Teal").performClick()
        onNodeWithContentDescription("Teal").assertIsSelected()
        onNodeWithText("Save").performClick()

        // The dialog closing is what says the save went through, so look at it before the repository.
        onNodeWithText("Colour").assertDoesNotExist()
        assertEquals("Flat 4C", spaceNow().name)
        assertEquals(2, spaceNow().accent)
    }

    @Test
    fun cannotSaveABlankName() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()

        onNode(hasSetTextAction()).performTextReplacement("   ")

        onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun cancelClosesTheDialogAndChangesNothing() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithContentDescription("Space settings").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Colour").assertDoesNotExist()
        assertEquals("Flat 4B", spaceNow().name)
    }

    @Test
    fun saysWhenTheChangesCouldNotBeSaved() = runComposeUiTest {
        val offline = object : SpaceRepository by spaces {
            override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) =
                error("Could not reach the server")
        }
        showSettingsAs("ana@orbit.app", offline)
        onNodeWithContentDescription("Space settings").performClick()
        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")

        onNodeWithText("Save").performClick()

        onNodeWithText("Could not save the changes. Try again.").assertIsDisplayed()
        onNodeWithText("Colour").assertIsDisplayed()
        assertEquals("Flat 4B", spaceNow().name)
    }
}
