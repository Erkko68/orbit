package com.orbit.app.feature.spacehome.members

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Role
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SpaceMembersTabTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()

    // The invitation card at the top of the tab gets its view model from Koin.
    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    /** Ana owns the space, Ben is an admin and Cy a plain member. Shows the tab as the given email. */
    private fun ComposeUiTest.showTabAs(email: String, changeRoleFailure: Exception? = null) {
        runBlocking { auth.signIn(email, "secret") }
        val spaces = FakeSpaceRepository.withMembers(
            "ana@orbit.app" to Role.OWNER,
            "ben@orbit.app" to Role.ADMIN,
            "cy@orbit.app" to Role.MEMBER,
            changeRoleFailure = changeRoleFailure,
        )
        startTestKoin(auth, spaces)
        setContent {
            OrbitTheme {
                SpaceMembersTab("space-1", viewModel = SpaceMembersViewModel("space-1", spaces, auth))
            }
        }
    }

    @Test
    fun theOwnerSeesTheControlOnEveryoneButThemselves() = runComposeUiTest {
        showTabAs("ana@orbit.app")

        onAllNodesWithText("Change role").assertCountEquals(2)
    }

    @Test
    fun anAdminSeesTheControlOnlyOnTheMember() = runComposeUiTest {
        showTabAs("ben@orbit.app")

        onAllNodesWithText("Change role").assertCountEquals(1)
    }

    @Test
    fun aPlainMemberDoesNotSeeTheControl() = runComposeUiTest {
        showTabAs("cy@orbit.app")

        onNodeWithText("cy@orbit.app").assertIsDisplayed()
        onAllNodesWithText("Change role").assertCountEquals(0)
    }

    @Test
    fun offersTheRolesAMemberCanBeGivenAndNotTheOneTheyHave() = runComposeUiTest {
        showTabAs("ana@orbit.app")

        onAllNodesWithText("Change role")[0].performClick()

        onNodeWithText("Role of ben@orbit.app").assertIsDisplayed()
        onNodeWithText("Make admin").assertIsNotEnabled()
        onNodeWithText("Make member").assertIsEnabled()
        onNodeWithText("Make owner").assertDoesNotExist()
    }

    @Test
    fun givesTheMemberTheRoleChosen() = runComposeUiTest {
        showTabAs("ana@orbit.app")
        onAllNodesWithText("Admin").assertCountEquals(1)

        onAllNodesWithText("Change role")[1].performClick()
        onNodeWithText("Make admin").performClick()

        onNodeWithText("Role of cy@orbit.app").assertDoesNotExist()
        onAllNodesWithText("Admin").assertCountEquals(2)
    }

    @Test
    fun cancelClosesThePickerAndChangesNothing() = runComposeUiTest {
        showTabAs("ana@orbit.app")
        onAllNodesWithText("Change role")[1].performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Role of cy@orbit.app").assertDoesNotExist()
        onAllNodesWithText("Admin").assertCountEquals(1)
    }

    @Test
    fun saysWhenTheRoleCouldNotBeChanged() = runComposeUiTest {
        showTabAs("ana@orbit.app", changeRoleFailure = IllegalStateException("Could not reach the server"))
        onAllNodesWithText("Change role")[1].performClick()

        onNodeWithText("Make admin").performClick()

        onNodeWithText("Could not change the role. Try again.").assertIsDisplayed()
        onNodeWithText("Role of cy@orbit.app").assertIsDisplayed()
        onAllNodesWithText("Admin").assertCountEquals(1)
    }
}
