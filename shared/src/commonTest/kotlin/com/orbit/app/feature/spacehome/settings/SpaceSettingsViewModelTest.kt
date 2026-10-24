package com.orbit.app.feature.spacehome.settings

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceSettingsViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)

    /** What the ViewModel asked the repository to write: the name and the colour, null when left alone. */
    private val writes = mutableListOf<Pair<String?, Int?>>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.settingsViewModel(repository: SpaceRepository = spaces) =
        SpaceSettingsViewModel("space-1", repository, auth).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    /** The real repository, noting what it is asked to write. */
    private fun recording() = object : SpaceRepository by spaces {
        override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) {
            writes += name to accent
            spaces.updateSpace(spaceId, name, accent)
        }
    }

    /** Ana owns Flat 4B, Ben is an admin of it and Cy a plain member. Ana is signed in. */
    private suspend fun flat() {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Flat 4B", accent = 1)
        spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.ADMIN))
        spaces.addMember("space-1", Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
    }

    private suspend fun spaceNow() = spaces.observeSpaces().first().single()

    @Test
    fun theOwnerAndAnAdminCanEditAndAPlainMemberCannot() = runTest {
        flat()
        assertTrue(settingsViewModel().uiState.value.canEdit)

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        assertTrue(settingsViewModel().uiState.value.canEdit)

        auth.signOut()
        auth.signIn("cy@orbit.app", "secret")
        assertFalse(settingsViewModel().uiState.value.canEdit)
    }

    @Test
    fun nobodyIsSignedInSoNothingCanBeEdited() = runTest {
        flat()
        auth.signOut()

        assertFalse(settingsViewModel().uiState.value.canEdit)
    }

    @Test
    fun opensWithTheNameAndColourTheSpaceHasNow() = runTest {
        flat()
        val viewModel = settingsViewModel()

        viewModel.open()

        val state = viewModel.uiState.value
        assertTrue(state.open)
        assertEquals("Flat 4B", state.name)
        assertEquals(1, state.accent)
        assertTrue(state.canSave)
    }

    @Test
    fun savesTheNewNameAndColourAndClosesTheDialog() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())
        viewModel.open()

        viewModel.onNameChange("  Flat 4C  ")
        viewModel.onAccentChange(3)
        viewModel.save()

        assertEquals(listOf<Pair<String?, Int?>>("Flat 4C" to 3), writes)
        assertEquals("Flat 4C", spaceNow().name)
        assertEquals(3, spaceNow().accent)
        assertFalse(viewModel.uiState.value.open)
        assertFalse(viewModel.uiState.value.saving)
    }

    @Test
    fun writesOnlyTheFieldThatChanged() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())

        viewModel.open()
        viewModel.onAccentChange(2)
        viewModel.save()
        viewModel.open()
        viewModel.onNameChange("Flat 4D")
        viewModel.save()

        assertEquals(listOf<Pair<String?, Int?>>(null to 2, "Flat 4D" to null), writes)
        assertEquals(2, spaceNow().accent)
    }

    @Test
    fun writesNothingWhenNothingChangedAndClosesTheDialog() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())
        viewModel.open()

        viewModel.save()

        assertTrue(writes.isEmpty())
        assertFalse(viewModel.uiState.value.open)
    }

    @Test
    fun cannotSaveABlankName() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())
        viewModel.open()

        viewModel.onNameChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
        viewModel.save()
        assertTrue(writes.isEmpty())
        assertTrue(viewModel.uiState.value.open)
    }

    @Test
    fun staysOpenAndSaysSoWhenSavingFails() = runTest {
        flat()
        val offline = object : SpaceRepository by spaces {
            override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) =
                error("Could not reach the server")
        }
        val viewModel = settingsViewModel(offline)
        viewModel.open()
        viewModel.onNameChange("Flat 4C")

        viewModel.save()

        val state = viewModel.uiState.value
        assertTrue(state.open)
        assertTrue(state.failed)
        assertFalse(state.saving)
        assertEquals("Flat 4C", state.name)
        assertEquals("Flat 4B", spaceNow().name)
    }

    @Test
    fun forgetsAFailureOnTheNextEdit() = runTest {
        flat()
        val offline = object : SpaceRepository by spaces {
            override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) =
                error("Could not reach the server")
        }
        val viewModel = settingsViewModel(offline)
        viewModel.open()
        viewModel.onAccentChange(2)
        viewModel.save()
        assertTrue(viewModel.uiState.value.failed)

        viewModel.onAccentChange(3)

        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun dismissingForgetsWhatWasTyped() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.open()
        viewModel.onNameChange("Something else")

        viewModel.dismiss()
        assertFalse(viewModel.uiState.value.open)

        viewModel.open()
        assertEquals("Flat 4B", viewModel.uiState.value.name)
    }

    @Test
    fun editingBeforeOpeningDoesNothing() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())

        viewModel.onNameChange("Flat 4C")
        viewModel.save()

        assertFalse(viewModel.uiState.value.open)
        assertTrue(writes.isEmpty())
    }

    @Test
    fun theOwnerDeletesAndEveryoneElseLeaves() = runTest {
        flat()
        assertTrue(settingsViewModel().uiState.value.deletes)

        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        val admin = settingsViewModel().uiState.value
        assertTrue(admin.visible)
        assertFalse(admin.deletes)

        auth.signOut()
        auth.signIn("cy@orbit.app", "secret")
        assertTrue(settingsViewModel().uiState.value.visible)
    }

    @Test
    fun nobodyOutsideTheSpaceSeesTheSettings() = runTest {
        flat()
        auth.signOut()
        auth.signIn("zoe@orbit.app", "secret")

        assertFalse(settingsViewModel().uiState.value.visible)
    }

    @Test
    fun aPlainMemberOpensStraightOnLeavingAndCancellingCloses() = runTest {
        flat()
        auth.signOut()
        auth.signIn("cy@orbit.app", "secret")
        val viewModel = settingsViewModel()

        viewModel.open()
        assertTrue(viewModel.uiState.value.confirmingExit)

        viewModel.cancelExit()
        assertFalse(viewModel.uiState.value.open)
    }

    @Test
    fun anOwnerBacksOutOfDeletingToTheSettings() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.open()

        viewModel.confirmExit()
        assertTrue(viewModel.uiState.value.confirmingExit)
        viewModel.cancelExit()

        assertTrue(viewModel.uiState.value.open)
        assertFalse(viewModel.uiState.value.confirmingExit)
    }

    @Test
    fun theOwnerDeletesTheSpace() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.open()
        viewModel.confirmExit()

        viewModel.exit()

        assertTrue(viewModel.uiState.value.gone)
        assertFalse(viewModel.uiState.value.open)
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun anAdminLeavesTheSpace() = runTest {
        flat()
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = settingsViewModel()
        viewModel.open()
        viewModel.confirmExit()

        viewModel.exit()

        assertTrue(viewModel.uiState.value.gone)
        assertEquals(emptyList(), spaces.observeSpaces().first())
        auth.signOut()
        auth.signIn("ana@orbit.app", "secret")
        assertEquals(listOf("ana@orbit.app", "cy@orbit.app"), spaceNow().memberIds)
    }

    @Test
    fun staysOpenAndSaysSoWhenDeletingFails() = runTest {
        flat()
        val failing = object : SpaceRepository by spaces {
            override suspend fun deleteSpace(spaceId: String) = error("Could not reach the server")
        }
        val viewModel = settingsViewModel(failing)
        viewModel.open()
        viewModel.confirmExit()

        viewModel.exit()

        val state = viewModel.uiState.value
        assertTrue(state.exitFailed)
        assertFalse(state.exiting)
        assertFalse(state.gone)
        assertTrue(state.confirmingExit)
    }

    @Test
    fun exitingWithoutConfirmingDoesNothing() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.open()

        viewModel.exit()

        assertFalse(viewModel.uiState.value.gone)
        assertEquals(1, spaces.observeSpaces().first().size)
    }
}
