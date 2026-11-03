package com.orbit.app.feature.member.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.member.MemberViewModel
import com.orbit.app.feature.spacehome.members.SpaceMembersViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MemberScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val opened = mutableListOf<String>()

    /** Ana owns Flat 4B. Ben is an admin of it and Cy a plain member, who has the bins. */
    private fun ComposeUiTest.showAs(email: String, userId: String, repository: SpaceRepository = spaces) {
        runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            spaces.createSpace("Flat 4B", accent = 1)
            spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.ADMIN))
            spaces.addMember("space-1", Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
            val bins = NewItem("Take the bins out", modules = ItemModules(assignment = Assignment.ToMembers(setOf("cy@orbit.app"))))
            items.create("space-1", bins)
            auth.signOut()
            auth.signIn(email, "secret")
        }
        setContent {
            OrbitTheme {
                MemberScreen(
                    spaceId = "space-1",
                    userId = userId,
                    onBack = {},
                    onOpenItem = { opened += it },
                    viewModel = MemberViewModel("space-1", userId, items, repository, auth),
                    membersViewModel = SpaceMembersViewModel("space-1", repository, auth),
                )
            }
        }
    }

    private fun roleOf(userId: String) = runBlocking { spaces.observeMembers("space-1").first().single { it.userId == userId }.role }

    @Test
    fun showsWhatTheyHaveAndOpensIt() = runComposeUiTest {
        showAs("cy@orbit.app", "cy@orbit.app")

        onNodeWithText("Cy").assertIsDisplayed()
        onNodeWithText("MEMBER").assertIsDisplayed()
        onNodeWithText("Has").performScrollTo().assertIsDisplayed()
        onNodeWithText("Take the bins out").performScrollTo().performClick()

        assertEquals(listOf("item-1"), opened)
    }

    @Test
    fun aPlainMemberSeesTheRoleButCannotChangeIt() = runComposeUiTest {
        showAs("cy@orbit.app", "ben@orbit.app")

        onNodeWithText("ADMIN").assertIsDisplayed()
        onNodeWithText("Nothing here yet").assertIsDisplayed()
        onNodeWithText("Admin").assertDoesNotExist()
    }

    @Test
    fun theOwnerOffersTheRolesAMemberCanBeGivenAndOwnership() = runComposeUiTest {
        showAs("ana@orbit.app", "ben@orbit.app")

        onNodeWithText("Admin").performClick()

        onNodeWithText("Role of Ben").assertIsDisplayed()
        onNodeWithText("Make admin").assertIsNotEnabled()
        onNodeWithText("Make member").assertIsEnabled()
        onNodeWithText("Make owner").assertIsEnabled()
    }

    @Test
    fun anAdminCannotHandTheSpaceOver() = runComposeUiTest {
        showAs("ben@orbit.app", "cy@orbit.app")

        onNodeWithText("Member").performClick()

        onNodeWithText("Make admin").assertIsEnabled()
        onNodeWithText("Make owner").assertDoesNotExist()
    }

    @Test
    fun givesTheMemberTheRoleChosen() = runComposeUiTest {
        showAs("ana@orbit.app", "cy@orbit.app")

        onNodeWithText("Member").performClick()
        onNodeWithText("Make admin").performClick()

        onNodeWithText("Role of Cy").assertDoesNotExist()
        assertEquals(Role.ADMIN, roleOf("cy@orbit.app"))
        onNodeWithText("Admin").assertIsDisplayed()
    }

    @Test
    fun handsTheSpaceOverAfterTheDialog() = runComposeUiTest {
        showAs("ana@orbit.app", "cy@orbit.app")

        onNodeWithText("Member").performClick()
        onNodeWithText("Make owner").performClick()
        onNodeWithText("Hand Flat 4B to Cy?").assertIsDisplayed()
        onNodeWithText("Hand it over").assertIsNotEnabled()
        onNodeWithContentDescription("I understand only Cy can give it back").performClick()
        onNodeWithText("Hand it over").assertIsEnabled().performClick()

        onNodeWithText("Role of Cy").assertDoesNotExist()
        assertEquals(Role.OWNER, roleOf("cy@orbit.app"))
        assertEquals(Role.ADMIN, roleOf("ana@orbit.app"))
    }

    @Test
    fun cancellingTheDialogChangesNothing() = runComposeUiTest {
        showAs("ana@orbit.app", "cy@orbit.app")
        onNodeWithText("Member").performClick()
        onNodeWithText("Make owner").performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Hand Flat 4B to Cy?").assertDoesNotExist()
        assertEquals(Role.MEMBER, roleOf("cy@orbit.app"))
    }

    @Test
    fun saysWhenTheRoleCouldNotBeChanged() = runComposeUiTest {
        val offline = object : SpaceRepository by spaces {
            override suspend fun changeRole(spaceId: String, userId: String, role: Role) =
                error("Could not reach the server")
        }
        showAs("ana@orbit.app", "cy@orbit.app", offline)
        onNodeWithText("Member").performClick()

        onNodeWithText("Make admin").performClick()

        onNodeWithText("Could not change the role. Try again.").assertIsDisplayed()
        onNodeWithText("Role of Cy").assertIsDisplayed()
    }
}
