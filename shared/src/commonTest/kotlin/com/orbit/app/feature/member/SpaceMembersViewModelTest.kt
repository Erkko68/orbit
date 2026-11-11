package com.orbit.app.feature.member

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.members_members
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceMembersViewModelTest {
    private val auth = InMemoryAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.membersViewModel(spaces: SpaceRepository) =
        SpaceMembersViewModel("space-1", spaces, auth).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    /** Ana owns the space, Ben is an admin and Cy a plain member. */
    private fun team(writeFailure: Exception? = null) = FakeSpaceRepository.withMembers(
        "ana@orbit.app" to Role.OWNER,
        "ben@orbit.app" to Role.ADMIN,
        "cy@orbit.app" to Role.MEMBER,
        writeFailure = writeFailure,
    )

    private fun SpaceMembersViewModel.canChange() = uiState.value.members.filter { it.canChangeRole }.map { it.id }

    private fun SpaceMembersViewModel.canRemove() = uiState.value.members.filter { it.canRemove }.map { it.id }

    @Test
    fun showsTheOwnerOfANewSpace() = runTest {
        val spaces = InMemorySpaceRepository(auth)
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceMembersViewModel(id, spaces, auth).uiState.first { it.members.isNotEmpty() }

        val owner = SpaceMember(
            id = "ana@orbit.app",
            index = 0,
            name = "Ana López",
            initials = "AL",
            photoUrl = null,
            role = Res.string.role_owner,
            currentRole = Role.OWNER,
            isYou = true,
        )
        assertEquals(listOf(owner), state.members)
    }

    @Test
    fun labelsEveryRole() = runTest {
        val repository = FakeSpaceRepository.withMembers("Ana" to Role.OWNER, "Ben" to Role.ADMIN, "Cy" to Role.MEMBER)

        val state = SpaceMembersViewModel("space-1", repository, auth).uiState.first { it.members.isNotEmpty() }

        assertEquals(
            listOf(Res.string.role_owner, Res.string.role_admin, Res.string.role_member),
            state.members.map { it.role },
        )
        assertEquals(listOf(Role.OWNER, Role.ADMIN, Role.MEMBER), state.members.map { it.currentRole })
    }

    @Test
    fun groupsTheMembersByRoleWithTheUserFirstThenByName() = runTest {
        auth.signIn("zoe@orbit.app", "secret")
        val repository = FakeSpaceRepository.withMembers(
            "cy@orbit.app" to Role.MEMBER,
            "ana@orbit.app" to Role.OWNER,
            "bo@orbit.app" to Role.MEMBER,
            "zoe@orbit.app" to Role.MEMBER,
        )

        val state = SpaceMembersViewModel("space-1", repository, auth).uiState.first { it.members.isNotEmpty() }

        assertEquals(listOf(Res.string.role_owner, Res.string.members_members), state.byRole.map { it.title })
        assertEquals(
            listOf("zoe@orbit.app", "bo@orbit.app", "cy@orbit.app"),
            state.byRole.last().members.map { it.id },
        )
        // Their place in the space keeps their colour, whichever section lists them.
        assertEquals(
            mapOf("cy@orbit.app" to 0, "ana@orbit.app" to 1, "bo@orbit.app" to 2, "zoe@orbit.app" to 3),
            state.members.associate { it.id to it.index },
        )
    }

    @Test
    fun saysWhenTheMembersFailToLoadAndLoadsThemOnRetry() = runTest {
        val viewModel = SpaceMembersViewModel("space-1", FlakySpaceRepository(memberFailures = 1), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loadFailed)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf("ana"), viewModel.uiState.value.members.map { it.name })
    }

    @Test
    fun onlyTheOwnerChangesRolesAndNeverTheirOwn() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assertEquals(listOf("ben@orbit.app", "cy@orbit.app"), membersViewModel(team()).canChange())

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertTrue(membersViewModel(team()).canChange().isEmpty())

        auth.signOut()
        auth.signIn("cy@orbit.app", "secret")
        assertTrue(membersViewModel(team()).canChange().isEmpty())
    }

    @Test
    fun nobodyIsSignedInSoNothingCanChange() = runTest {
        assertTrue(membersViewModel(team()).canChange().isEmpty())
    }

    @Test
    fun makesAMemberAdminAndAnAdminMember() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team())

        viewModel.changeRole("cy@orbit.app", Role.ADMIN)
        viewModel.changeRole("ben@orbit.app", Role.MEMBER)

        val state = viewModel.uiState.value
        assertEquals(Role.ADMIN, state.members.single { it.id == "cy@orbit.app" }.currentRole)
        assertEquals(Role.MEMBER, state.members.single { it.id == "ben@orbit.app" }.currentRole)
        assertFalse(state.busy)
        assertFalse(state.failed)
    }

    @Test
    fun saysOnTheirPageWhenTheChangeFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))

        viewModel.changeRole("cy@orbit.app", Role.ADMIN)

        val state = viewModel.uiState.value
        assertTrue(state.roleFailed("cy@orbit.app"))
        assertFalse(state.roleFailed("ben@orbit.app"))
        assertFalse(state.busy)
        assertEquals(Role.MEMBER, state.members.single { it.id == "cy@orbit.app" }.currentRole)
    }

    @Test
    fun anAdminCannotChangeARoleNorMakeAnOwnerThatWay() = runTest {
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))
        viewModel.changeRole("cy@orbit.app", Role.ADMIN)
        assertFalse(viewModel.uiState.value.failed)

        auth.signOut()
        auth.signIn("ana@orbit.app", "secret")
        viewModel.changeRole("cy@orbit.app", Role.OWNER)
        viewModel.changeRole("cy@orbit.app", Role.MEMBER)
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun theOwnerHandsTheSpaceOnlyToAnAdmin() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val owner = membersViewModel(team()).uiState.value.members
        assertEquals(listOf("ben@orbit.app"), owner.filter { it.canTakeOwnership }.map { it.id })

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertTrue(membersViewModel(team()).uiState.value.members.none { it.canTakeOwnership })
    }

    @Test
    fun handsTheSpaceOverAndClosesTheDialog() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team())
        viewModel.askTransfer("ben@orbit.app")
        viewModel.acknowledgeTransfer(true)

        viewModel.transferOwnership()

        val state = viewModel.uiState.value
        assertNull(state.transferTo)
        assertEquals(Role.OWNER, state.members.single { it.id == "ben@orbit.app" }.currentRole)
        assertEquals(Role.ADMIN, state.members.single { it.id == "ana@orbit.app" }.currentRole)
    }

    @Test
    fun keepsTheDialogOpenWhenTheHandoverFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))
        viewModel.askTransfer("ben@orbit.app")
        viewModel.acknowledgeTransfer(true)

        viewModel.transferOwnership()

        assertTrue(viewModel.uiState.value.roleFailed("ben@orbit.app"))
        assertEquals("ben@orbit.app", viewModel.uiState.value.transferTo?.id)
        assertEquals(Role.OWNER, viewModel.uiState.value.members.single { it.id == "ana@orbit.app" }.currentRole)
    }

    @Test
    fun asksNothingForAPlainMemberOrForAnAdminWhoIsNotTheOwner() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val owner = membersViewModel(team())
        owner.askTransfer("cy@orbit.app")
        assertNull(owner.uiState.value.transferTo)

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        val admin = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))
        admin.askTransfer("cy@orbit.app")
        admin.transferOwnership()
        assertNull(admin.uiState.value.transferTo)
        assertFalse(admin.uiState.value.failed)
    }

    @Test
    fun handsNothingOverUntilTheOwnerAcknowledgesIt() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))

        viewModel.transferOwnership()
        assertFalse(viewModel.uiState.value.failed)

        viewModel.askTransfer("ben@orbit.app")
        assertFalse(viewModel.uiState.value.canTransfer)
        viewModel.transferOwnership()
        assertFalse(viewModel.uiState.value.failed)

        viewModel.acknowledgeTransfer(true)
        assertTrue(viewModel.uiState.value.canTransfer)
    }

    @Test
    fun cancellingTheDialogForgetsTheTickAndChangesNothing() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team())
        viewModel.askTransfer("ben@orbit.app")
        viewModel.acknowledgeTransfer(true)

        viewModel.cancelTransfer()

        val state = viewModel.uiState.value
        assertNull(state.transferTo)
        assertFalse(state.transferAcknowledged)
        assertEquals(Role.OWNER, state.members.single { it.id == "ana@orbit.app" }.currentRole)
    }

    @Test
    fun theOwnerRemovesAnyoneElseAndAnAdminOnlyAPlainMember() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        assertEquals(listOf("ben@orbit.app", "cy@orbit.app"), membersViewModel(team()).canRemove())

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertEquals(listOf("cy@orbit.app"), membersViewModel(team()).canRemove())

        auth.signOut()
        auth.signIn("cy@orbit.app", "secret")
        assertTrue(membersViewModel(team()).canRemove().isEmpty())
    }

    @Test
    fun nobodyIsSignedInSoNobodyCanBeRemoved() = runTest {
        assertTrue(membersViewModel(team()).canRemove().isEmpty())
    }

    @Test
    fun takesAMemberOutOfTheSpaceOnceTheDialogIsConfirmed() = runTest {
        val spaces = InMemorySpaceRepository(auth)
        auth.register("ana@orbit.app", "secret", displayName = "Ana")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(id, Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
        val viewModel = membersViewModel(spaces)

        viewModel.askRemove("cy@orbit.app")
        assertEquals("cy@orbit.app", viewModel.uiState.value.removing?.id)
        viewModel.removeMember()

        assertNull(viewModel.uiState.value.removing)
        assertEquals(listOf("ana@orbit.app"), viewModel.uiState.value.members.map { it.id })
    }

    @Test
    fun keepsTheDialogOpenAndSaysSoWhenRemovingFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))

        viewModel.askRemove("cy@orbit.app")
        viewModel.removeMember()

        val state = viewModel.uiState.value
        assertEquals("cy@orbit.app", state.removing?.id)
        assertTrue(state.roleFailed("cy@orbit.app"))
        assertFalse(state.busy)
    }

    @Test
    fun aPlainMemberCannotEvenOpenTheRemoveDialog() = runTest {
        auth.signIn("cy@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))

        viewModel.askRemove("ben@orbit.app")

        assertNull(viewModel.uiState.value.removing)
    }

    @Test
    fun confirmingWithNoDialogOpenRemovesNobody() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))

        viewModel.removeMember()

        assertNull(viewModel.uiState.value.removing)
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun cancellingTheDialogLeavesEveryoneWhereTheyWere() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))

        viewModel.askRemove("cy@orbit.app")
        viewModel.cancelRemove()

        assertNull(viewModel.uiState.value.removing)
        assertEquals(3, viewModel.uiState.value.members.size)
    }

}
