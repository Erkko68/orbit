package com.orbit.app.feature.spacesettings.ui

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
import com.orbit.app.core.share.TextSharer
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.feature.spacesettings.SpaceSettingsViewModel
import com.orbit.app.feature.spacesettings.invite.InviteViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpaceSettingsScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private var gone = 0
    private val opened = mutableListOf<String>()

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
                SpaceSettingsScreen(
                    "space-1",
                    onBack = {},
                    onOpenMember = { opened += it },
                    onGone = { gone++ },
                    viewModel = SpaceSettingsViewModel("space-1", repository, auth),
                    membersViewModel = SpaceMembersViewModel("space-1", repository, auth),
                    inviteViewModel = InviteViewModel("space-1", InMemoryInvitationRepository(auth), repository, auth),
                    sharer = TextSharer {},
                )
            }
        }
    }

    private fun spaceNow() = runBlocking { spaces.observeSpaces().first().single() }

    @Test
    fun theOwnerSeesEverythingTheyCanDo() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithContentDescription("Badge 2").assertIsSelected()
        onNodeWithContentDescription("Badge 1").assertIsNotSelected()
        onNodeWithText("Save").assertDoesNotExist()
        onNodeWithText("Invite people").performScrollTo().assertIsDisplayed()
        onNodeWithText("Delete space").performScrollTo().assertIsDisplayed()
        onNodeWithText("Leave space").assertDoesNotExist()
    }

    @Test
    fun aPlainMemberSeesTheMembersAndLeaving() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")

        onNodeWithText("Flat 4B").assertDoesNotExist()
        onNodeWithText("Invite people").assertDoesNotExist()
        onNodeWithText("Members").assertIsDisplayed()
        onNodeWithText("You").performScrollTo().assertIsDisplayed()
        onNodeWithText("Leave space").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theMembersListTheOwnerFirstWithTheirRoles() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")

        onNodeWithText("3").assertIsDisplayed()
        onNodeWithText("Owner").assertIsDisplayed()
        onNodeWithText("Admin").assertIsDisplayed()
        onNodeWithText("Member").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aMemberRowOpensTheirScreen() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNodeWithText("Ben").performScrollTo().performClick()

        assertEquals(listOf("ben@orbit.app"), opened)
    }

    @Test
    fun invitesInASheet() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNodeWithText("Invite people").performScrollTo().performClick()

        onNodeWithText("Create invitation").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun savesTheNewNameAndColour() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")
        onNodeWithContentDescription("Badge 3").performClick()
        onNodeWithContentDescription("Badge 3").assertIsSelected()
        onNodeWithText("Save").performScrollTo().performClick()

        // Save going away is what says the save went through, so look at it before the repository.
        onNodeWithText("Save").assertDoesNotExist()
        assertEquals("Flat 4C", spaceNow().name)
        assertEquals(2, spaceNow().accent)
    }

    @Test
    fun cannotSaveABlankName() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNode(hasSetTextAction()).performTextReplacement("   ")

        onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun saysWhenTheChangesCouldNotBeSaved() = runComposeUiTest {
        val offline = object : SpaceRepository by spaces {
            override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) =
                error("Could not reach the server")
        }
        showSettingsAs("ana@orbit.app", offline)
        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")

        onNodeWithText("Save").performScrollTo().performClick()

        onNodeWithText("Could not save the changes. Try again.").assertIsDisplayed()
        onNodeWithText("Save").assertIsEnabled()
        assertEquals("Flat 4B", spaceNow().name)
    }

    @Test
    fun aMemberLeavesAfterConfirming() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        onNodeWithText("Leave space").performScrollTo().performClick()

        onNodeWithText("Leave").performClick()

        waitForIdle()
        assertEquals(1, gone)
        val members = runBlocking { spaces.observeMembers("space-1").first() }
        assertEquals(listOf("ana@orbit.app", "ben@orbit.app"), members.map { it.userId })
    }

    @Test
    fun theOwnerDeletesTheSpaceAfterConfirming() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        onNodeWithText("Delete space").performScrollTo().performClick()
        onNodeWithText("Delete this space?").assertIsDisplayed()
        onNodeWithText("Delete").performClick()

        waitForIdle()
        assertEquals(1, gone)
        assertEquals(emptyList(), runBlocking { spaces.observeSpaces().first() })
    }

    @Test
    fun cancellingTheDeleteChangesNothing() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onNodeWithText("Delete space").performScrollTo().performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Delete this space?").assertDoesNotExist()
        assertEquals(0, gone)
        assertEquals("Flat 4B", spaceNow().name)
    }

    @Test
    fun saysWhenLeavingFailed() = runComposeUiTest {
        val failing = object : SpaceRepository by spaces {
            override suspend fun leaveSpace(spaceId: String) = error("Could not reach the server")
        }
        showSettingsAs("cy@orbit.app", failing)
        onNodeWithText("Leave space").performScrollTo().performClick()

        onNodeWithText("Leave").performClick()

        onNodeWithText("Could not leave the space. Try again.").assertIsDisplayed()
        assertEquals(0, gone)
    }
}
