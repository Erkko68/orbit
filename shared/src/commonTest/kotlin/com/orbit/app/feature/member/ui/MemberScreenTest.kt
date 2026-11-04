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
import com.orbit.app.feature.member.SpaceMembersViewModel
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MemberScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val opened = mutableListOf<String>()

    /**
     * Ana owns Flat 4B. Ben is an admin of it and Cy a plain member, who has the bins. What a member has resolves its
     * view model through Koin.
     */
    private fun ComposeUiTest.showMemberAs(email: String, userId: String, repository: SpaceRepository = spaces) {
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
        startTestKoin(auth, repository, items)
        setContent {
            OrbitTheme {
                MemberScreen(
                    "space-1",
                    userId,
                    onBack = {},
                    onOpenItem = { opened += it },
                    viewModel = SpaceMembersViewModel("space-1", repository, auth),
                )
            }
        }
    }

    private fun roleOf(userId: String) = runBlocking { spaces.observeMembers("space-1").first().single { it.userId == userId }.role }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun showsWhoTheyAreAndWhatTheyHaveWhichOpensTheItem() = runComposeUiTest {
        showMemberAs("cy@orbit.app", "cy@orbit.app")

        onNodeWithText("Cy").assertIsDisplayed()
        onNodeWithText("MEMBER").assertIsDisplayed()
        onNodeWithText("Has").performScrollTo().assertIsDisplayed()
        onNodeWithText("Take the bins out").performScrollTo().performClick()

        assertEquals(listOf("item-1"), opened)
    }

    @Test
    fun onlyTheOwnerSeesTheRoleRow() = runComposeUiTest {
        showMemberAs("ben@orbit.app", "cy@orbit.app")

        onNodeWithText("MEMBER").assertIsDisplayed()
        onNodeWithText("Role").assertDoesNotExist()
        onNodeWithText("Nothing here yet").assertDoesNotExist()
    }

    @Test
    fun theOwnerMakesAMemberAdminFromTheSheet() = runComposeUiTest {
        showMemberAs("ana@orbit.app", "cy@orbit.app")

        onNodeWithText("Role").performClick()
        onNodeWithText("Make owner").assertDoesNotExist()
        onNodeWithText("Admin").performClick()

        waitForIdle()
        assertEquals(Role.ADMIN, roleOf("cy@orbit.app"))
        onNodeWithText("ADMIN").assertIsDisplayed()
    }

    @Test
    fun theOwnerMakesAnAdminAMemberAgain() = runComposeUiTest {
        showMemberAs("ana@orbit.app", "ben@orbit.app")

        onNodeWithText("Role").performClick()
        onNodeWithText("Member").performClick()

        waitForIdle()
        assertEquals(Role.MEMBER, roleOf("ben@orbit.app"))
    }

    @Test
    fun handsTheSpaceOverAfterTheDialog() = runComposeUiTest {
        showMemberAs("ana@orbit.app", "ben@orbit.app")
        onNodeWithText("Role").performClick()

        onNodeWithText("Make owner").performClick()
        onNodeWithText("Hand Flat 4B to Ben?").assertIsDisplayed()
        onNodeWithText("Hand it over").assertIsNotEnabled()
        onNodeWithContentDescription("I understand only Ben can give it back").performClick()
        onNodeWithText("Hand it over").assertIsEnabled().performClick()

        onNodeWithText("Hand Flat 4B to Ben?").assertDoesNotExist()
        assertEquals(Role.OWNER, roleOf("ben@orbit.app"))
        assertEquals(Role.ADMIN, roleOf("ana@orbit.app"))
    }

    @Test
    fun cancellingTheHandoverChangesNothing() = runComposeUiTest {
        showMemberAs("ana@orbit.app", "ben@orbit.app")
        onNodeWithText("Role").performClick()
        onNodeWithText("Make owner").performClick()

        onNodeWithText("Cancel").performClick()

        onNodeWithText("Hand Flat 4B to Ben?").assertDoesNotExist()
        assertEquals(Role.ADMIN, roleOf("ben@orbit.app"))
    }

    @Test
    fun saysWhenTheRoleCouldNotBeChanged() = runComposeUiTest {
        val offline = object : SpaceRepository by spaces {
            override suspend fun changeRole(spaceId: String, userId: String, role: Role) =
                error("Could not reach the server")
        }
        showMemberAs("ana@orbit.app", "cy@orbit.app", offline)
        onNodeWithText("Role").performClick()

        onNodeWithText("Admin").performClick()

        onNodeWithText("Could not change the role. Try again.").assertIsDisplayed()
        assertEquals(Role.MEMBER, roleOf("cy@orbit.app"))
    }

    @Test
    fun saysWhenTheMemberIsGone() = runComposeUiTest {
        showMemberAs("ana@orbit.app", "someone@orbit.app")

        onNodeWithText("Member not found").assertIsDisplayed()
    }
}
