package com.orbit.app.feature.spacehome.members

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.share.TextSharer
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryInvitationRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpaceMembersRowTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    private fun ComposeUiTest.showRow(spaces: SpaceRepository) = setContent {
        OrbitTheme {
            SpaceMembersRow(
                spaceId = "space-1",
                viewModel = SpaceMembersViewModel("space-1", spaces, auth),
                inviteViewModel = InviteViewModel("space-1", InMemoryInvitationRepository(auth), spaces, auth),
                sharer = TextSharer {},
            )
        }
    }

    /** Ana owns the space, Ben is an admin and Cy a plain member. Shows the row as the given email. */
    private fun ComposeUiTest.showRowAs(email: String, writeFailure: Exception? = null) {
        runBlocking { auth.signIn(email, "secret") }
        showRow(
            FakeSpaceRepository.withMembers(
                "ana@orbit.app" to Role.OWNER,
                "ben@orbit.app" to Role.ADMIN,
                "cy@orbit.app" to Role.MEMBER,
                writeFailure = writeFailure,
            ),
        )
    }

    @Test
    fun showsEveryMemberWithTheirRoleAndTheUserAsYou() = runComposeUiTest {
        showRowAs("cy@orbit.app")

        onNodeWithText("ana@orbit.app").assertIsDisplayed()
        onNodeWithText("ben@orbit.app").assertIsDisplayed()
        onNodeWithText("You").assertIsDisplayed()
        onNodeWithText("Owner").assertIsDisplayed()
        onNodeWithText("cy@orbit.app").assertDoesNotExist()
    }

    @Test
    fun anOwnerOrAdminCanInviteAndAPlainMemberCannot() = runComposeUiTest {
        showRowAs("cy@orbit.app")
        onNodeWithText("Invite").assertDoesNotExist()
    }

    @Test
    fun theOwnerSeesTheInviteChip() = runComposeUiTest {
        showRowAs("ana@orbit.app")
        onNodeWithText("Invite").assertIsDisplayed()
    }

    @Test
    fun aPlainMemberCannotOpenAnotherMember() = runComposeUiTest {
        showRowAs("cy@orbit.app")

        onNodeWithText("ben@orbit.app").assertHasNoClickAction()
    }

    @Test
    fun theOwnerOffersTheRolesAMemberCanBeGivenAndOwnership() = runComposeUiTest {
        showRowAs("ana@orbit.app")

        onNodeWithText("ben@orbit.app").performClick()

        onNodeWithText("Role of ben@orbit.app").assertIsDisplayed()
        onNodeWithText("Make admin").assertIsNotEnabled()
        onNodeWithText("Make member").assertIsEnabled()
        onNodeWithText("Make owner").assertIsEnabled()
    }

    @Test
    fun anAdminCannotHandTheSpaceOver() = runComposeUiTest {
        showRowAs("ben@orbit.app")

        onNodeWithText("cy@orbit.app").performClick()

        onNodeWithText("Make admin").assertIsEnabled()
        onNodeWithText("Make owner").assertDoesNotExist()
    }

    @Test
    fun givesTheMemberTheRoleChosen() = runComposeUiTest {
        showRowAs("ana@orbit.app")
        onAllNodesWithText("Admin").assertCountEquals(1)

        onNodeWithText("cy@orbit.app").performClick()
        onNodeWithText("Make admin").performClick()

        onNodeWithText("Role of cy@orbit.app").assertDoesNotExist()
        onAllNodesWithText("Admin").assertCountEquals(2)
    }

    @Test
    fun handsTheSpaceOverAndTheOwnerBecomesAnAdmin() = runComposeUiTest {
        showRowAs("ana@orbit.app")

        onNodeWithText("cy@orbit.app").performClick()
        onNodeWithText("Make owner").performClick()

        onNodeWithText("Role of cy@orbit.app").assertDoesNotExist()
        onAllNodesWithText("Owner").assertCountEquals(1)
        onAllNodesWithText("Admin").assertCountEquals(2)
    }

    @Test
    fun cancelClosesThePickerAndChangesNothing() = runComposeUiTest {
        showRowAs("ana@orbit.app")
        onNodeWithText("cy@orbit.app").performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Role of cy@orbit.app").assertDoesNotExist()
        onAllNodesWithText("Admin").assertCountEquals(1)
    }

    @Test
    fun saysWhenTheRoleCouldNotBeChanged() = runComposeUiTest {
        showRowAs("ana@orbit.app", writeFailure = IllegalStateException("Could not reach the server"))
        onNodeWithText("cy@orbit.app").performClick()

        onNodeWithText("Make admin").performClick()

        onNodeWithText("Could not change the role. Try again.").assertIsDisplayed()
        onNodeWithText("Role of cy@orbit.app").assertIsDisplayed()
        onAllNodesWithText("Admin").assertCountEquals(1)
    }

    @Test
    fun theUsersOwnChipIsNotTappable() = runComposeUiTest {
        showRowAs("ana@orbit.app")

        onNodeWithText("You").assertHasNoClickAction()
    }
}
