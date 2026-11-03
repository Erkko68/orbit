package com.orbit.app.feature.spacehome.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
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
import com.orbit.app.feature.spacehome.members.InviteViewModel
import com.orbit.app.feature.spacehome.members.SpaceMembersViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.hasClickLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpaceSettingsTest : ComposeTest() {
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
                SpaceSettings(
                    "space-1",
                    onGone = { gone++ },
                    onOpenMember = { opened += it },
                    viewModel = SpaceSettingsViewModel("space-1", repository, auth),
                    membersViewModel = SpaceMembersViewModel("space-1", repository, auth),
                    inviteViewModel = InviteViewModel("space-1", InMemoryInvitationRepository(auth), repository, auth),
                    sharer = TextSharer {},
                )
            }
        }
    }

    private fun spaceNow() = runBlocking { spaces.observeSpaces().first().single() }

    private fun ComposeUiTest.openSettings() = onAllNodes(hasClickLabel("Space settings")).onFirst().performClick()

    private fun ComposeUiTest.openIdentity() {
        openSettings()
        onNodeWithText("Name and colour").performScrollTo().performClick()
    }

    @Test
    fun everyMemberSeesTheButton() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        onAllNodes(hasClickLabel("Space settings")).onFirst().assertIsDisplayed()
    }

    @Test
    fun theOwnerSeesEverythingTheyCanDo() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        openSettings()

        onNodeWithText("Invite people").assertIsDisplayed()
        onNodeWithText("Members and roles").assertIsDisplayed()
        onNodeWithText("3 members").assertIsDisplayed()
        onNodeWithText("Name and colour").assertIsDisplayed()
        onNodeWithText("Delete space").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aPlainMemberSeesTheMembersAndLeavingOnly() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        openSettings()

        onNodeWithText("Members and roles").assertIsDisplayed()
        onNodeWithText("Invite people").assertDoesNotExist()
        onNodeWithText("Name and colour").assertDoesNotExist()
        onNodeWithText("Leave space").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun invitesFromTheMenu() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        openSettings()

        onNodeWithText("Invite people").performClick()

        onNodeWithText("Create invitation").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aMemberLeavesAfterConfirming() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        openSettings()
        onNodeWithText("Leave space").performScrollTo().performClick()

        onNodeWithText("Leave").performClick()

        waitForIdle()
        assertEquals(1, gone)
        val members = runBlocking { spaces.observeMembers("space-1").first() }
        assertEquals(listOf("ana@orbit.app", "ben@orbit.app"), members.map { it.userId })
    }

    @Test
    fun anAdminLeavesFromTheSettings() = runComposeUiTest {
        showSettingsAs("ben@orbit.app")
        openSettings()

        onNodeWithText("Leave space").performScrollTo().performClick()
        onNodeWithText("Leave").performClick()

        waitForIdle()
        assertEquals(1, gone)
    }

    @Test
    fun theMembersAreGroupedByRole() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        openSettings()
        onNodeWithText("Members and roles").performClick()

        onNodeWithText("Owner").assertIsDisplayed()
        onNodeWithText("Admins").assertIsDisplayed()
        onNodeWithText("Members").assertIsDisplayed()
        onNodeWithText("You").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingAMemberOpensTheirPage() = runComposeUiTest {
        showSettingsAs("cy@orbit.app")
        openSettings()
        onNodeWithText("Members and roles").performClick()

        onNodeWithText("Ben").performScrollTo().performClick()

        assertEquals(listOf("ben@orbit.app"), opened)
        onNodeWithText("Ben").assertDoesNotExist()
    }

    @Test
    fun theUserSeesThemselvesAsYou() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        openSettings()
        onNodeWithText("Members and roles").performClick()

        onNodeWithText("You").performScrollTo().assertIsDisplayed()
        onNodeWithText("Owner").assertIsDisplayed()
    }

    @Test
    fun theOwnerDeletesTheSpaceAfterConfirming() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        onAllNodes(hasClickLabel("Space settings")).onFirst().performClick()
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
        onAllNodes(hasClickLabel("Space settings")).onFirst().performClick()
        onNodeWithText("Delete space").performScrollTo().performClick()

        onNodeWithContentDescription("Back").performClick()

        onNodeWithText("Delete this space?").assertDoesNotExist()
        onNodeWithText("Name and colour").assertIsDisplayed()
        assertEquals(0, gone)
    }

    @Test
    fun saysWhenLeavingFailed() = runComposeUiTest {
        val failing = object : SpaceRepository by spaces {
            override suspend fun leaveSpace(spaceId: String) = error("Could not reach the server")
        }
        showSettingsAs("cy@orbit.app", failing)
        openSettings()
        onNodeWithText("Leave space").performScrollTo().performClick()

        onNodeWithText("Leave").performClick()

        onNodeWithText("Could not leave the space. Try again.").assertIsDisplayed()
        assertEquals(0, gone)
    }

    @Test
    fun opensWithTheCurrentNameAndColourPicked() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")

        openIdentity()

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithContentDescription("Coral").assertIsSelected()
        onNodeWithContentDescription("Violet").assertIsNotSelected()
        onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun savesTheNewNameAndColour() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        openIdentity()

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
        openIdentity()

        onNode(hasSetTextAction()).performTextReplacement("   ")

        onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun cancelGoesBackToTheMenuAndChangesNothing() = runComposeUiTest {
        showSettingsAs("ana@orbit.app")
        openIdentity()
        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")

        onNodeWithContentDescription("Back").performClick()

        onNodeWithText("Colour").assertDoesNotExist()
        onNodeWithText("Name and colour").assertIsDisplayed()
        assertEquals("Flat 4B", spaceNow().name)
    }

    @Test
    fun saysWhenTheChangesCouldNotBeSaved() = runComposeUiTest {
        val offline = object : SpaceRepository by spaces {
            override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) =
                error("Could not reach the server")
        }
        showSettingsAs("ana@orbit.app", offline)
        openIdentity()
        onNode(hasSetTextAction()).performTextReplacement("Flat 4C")

        onNodeWithText("Save").performClick()

        onNodeWithText("Could not save the changes. Try again.").assertIsDisplayed()
        onNodeWithText("Colour").assertIsDisplayed()
        assertEquals("Flat 4B", spaceNow().name)
    }
}
