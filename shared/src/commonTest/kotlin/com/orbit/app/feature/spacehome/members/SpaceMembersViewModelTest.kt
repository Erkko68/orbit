package com.orbit.app.feature.spacehome.members

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
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    @Test
    fun showsTheOwnerOfANewSpace() = runTest {
        val spaces = InMemorySpaceRepository(auth)
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceMembersViewModel(id, spaces, auth).uiState.first { it.members.isNotEmpty() }

        val owner = SpaceMember(
            id = "ana@orbit.app",
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
    fun saysWhenTheMembersFailToLoadAndLoadsThemOnRetry() = runTest {
        val viewModel = SpaceMembersViewModel("space-1", FlakySpaceRepository(memberFailures = 1), auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loadFailed)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals(listOf("ana"), viewModel.uiState.value.members.map { it.name })
    }

    @Test
    fun theOwnerCanChangeEveryRoleButTheirOwn() = runTest {
        auth.signIn("ana@orbit.app", "secret")

        assertEquals(listOf("ben@orbit.app", "cy@orbit.app"), membersViewModel(team()).canChange())
    }

    @Test
    fun anAdminCanChangeEveryRoleButTheOwnerAndTheirOwn() = runTest {
        auth.signIn("ben@orbit.app", "secret")

        assertEquals(listOf("cy@orbit.app"), membersViewModel(team()).canChange())
    }

    @Test
    fun aPlainMemberCannotChangeAnyRole() = runTest {
        auth.signIn("cy@orbit.app", "secret")

        assertTrue(membersViewModel(team()).canChange().isEmpty())
    }

    @Test
    fun nobodyIsSignedInSoNothingCanChange() = runTest {
        assertTrue(membersViewModel(team()).canChange().isEmpty())
    }

    @Test
    fun givesAMemberTheRoleChosenAndClosesThePicker() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team())
        viewModel.openRolePicker("cy@orbit.app")
        assertEquals("cy@orbit.app", viewModel.uiState.value.rolePicker?.id)

        viewModel.changeRole(Role.ADMIN)

        val state = viewModel.uiState.value
        assertEquals(Role.ADMIN, state.members.single { it.id == "cy@orbit.app" }.currentRole)
        assertNull(state.rolePicker)
        assertFalse(state.busy)
        assertFalse(state.failed)
    }

    @Test
    fun keepsThePickerOpenAndSaysSoWhenTheChangeFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))
        viewModel.openRolePicker("cy@orbit.app")

        viewModel.changeRole(Role.ADMIN)

        val state = viewModel.uiState.value
        assertNotNull(state.rolePicker)
        assertTrue(state.failed)
        assertFalse(state.busy)
        assertEquals(Role.MEMBER, state.members.single { it.id == "cy@orbit.app" }.currentRole)
    }

    @Test
    fun forgetsAFailureWhenThePickerClosesAndOpensAgain() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))
        viewModel.openRolePicker("cy@orbit.app")
        viewModel.changeRole(Role.ADMIN)

        viewModel.closeRolePicker()
        assertNull(viewModel.uiState.value.rolePicker)
        assertFalse(viewModel.uiState.value.failed)

        viewModel.openRolePicker("cy@orbit.app")
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun doesNotOpenThePickerForAMemberWhoseRoleCannotChange() = runTest {
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = membersViewModel(team())

        viewModel.openRolePicker("ana@orbit.app")
        assertNull(viewModel.uiState.value.rolePicker)

        viewModel.openRolePicker("ben@orbit.app")
        assertNull(viewModel.uiState.value.rolePicker)
    }

    @Test
    fun changingARoleWithNoPickerOpenDoesNothing() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))

        viewModel.changeRole(Role.ADMIN)

        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun onlyTheOwnerCanHandTheSpaceToAnyoneElse() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val owner = membersViewModel(team()).uiState.value.members
        assertEquals(listOf("ben@orbit.app", "cy@orbit.app"), owner.filter { it.canTakeOwnership }.map { it.id })

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertTrue(membersViewModel(team()).uiState.value.members.none { it.canTakeOwnership })
    }

    @Test
    fun handsTheSpaceOverAndClosesThePicker() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team())
        viewModel.openRolePicker("cy@orbit.app")

        viewModel.transferOwnership()

        val state = viewModel.uiState.value
        assertEquals(Role.OWNER, state.members.single { it.id == "cy@orbit.app" }.currentRole)
        assertEquals(Role.ADMIN, state.members.single { it.id == "ana@orbit.app" }.currentRole)
        assertNull(state.rolePicker)
    }

    @Test
    fun keepsThePickerOpenWhenTheHandoverFails() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Could not reach the server")))
        viewModel.openRolePicker("cy@orbit.app")

        viewModel.transferOwnership()

        assertTrue(viewModel.uiState.value.failed)
        assertNotNull(viewModel.uiState.value.rolePicker)
        assertEquals(Role.OWNER, viewModel.uiState.value.members.single { it.id == "ana@orbit.app" }.currentRole)
    }

    @Test
    fun anAdminCannotHandTheSpaceOver() = runTest {
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = membersViewModel(team(writeFailure = IllegalStateException("Must not be called")))
        viewModel.openRolePicker("cy@orbit.app")

        viewModel.transferOwnership()

        assertFalse(viewModel.uiState.value.failed)
    }
}
