package com.orbit.app.feature.spacesettings

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

    /** The names the ViewModel asked the repository to write. */
    private val writes = mutableListOf<String>()

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
        override suspend fun renameSpace(spaceId: String, name: String) {
            writes += name
            spaces.renameSpace(spaceId, name)
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
        val viewModel = settingsViewModel()
        assertTrue(viewModel.uiState.value.canEdit)
        assertTrue(viewModel.uiState.value.deletes)

        auth.signIn("ben@orbit.app", "secret")
        assertTrue(viewModel.uiState.value.canEdit)
        assertFalse(viewModel.uiState.value.deletes)

        auth.signIn("cy@orbit.app", "secret")
        assertFalse(viewModel.uiState.value.canEdit)
    }

    @Test
    fun nobodyIsSignedInSoNothingCanBeEditedOrLeft() = runTest {
        val viewModel = settingsViewModel()
        assertFalse(viewModel.uiState.value.canEdit)
        viewModel.confirmExit()
        assertFalse(viewModel.uiState.value.confirmingExit)
    }

    @Test
    fun showsTheNameTheSpaceHasNow() = runTest {
        flat()
        val state = settingsViewModel().uiState.value
        assertEquals("Flat 4B", state.name)
        assertFalse(state.changed)
        assertFalse(state.canSave)
    }

    @Test
    fun savesTheNewNameThenShowsWhatIsSaved() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())
        viewModel.onNameChange("  Flat 5C ")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.save()

        assertEquals(listOf("Flat 5C"), writes)
        assertEquals("Flat 5C", spaceNow().name)
        val state = viewModel.uiState.value
        assertEquals("Flat 5C", state.name)
        assertFalse(state.changed)
        assertFalse(state.saving)
    }

    @Test
    fun nothingChangedOrABlankNameCannotBeSaved() = runTest {
        flat()
        val viewModel = settingsViewModel(recording())
        viewModel.onNameChange("Flat 4B ")
        assertFalse(viewModel.uiState.value.changed)
        viewModel.onNameChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.save()

        assertTrue(writes.isEmpty())
    }

    @Test
    fun aPlainMemberCannotSave() = runTest {
        flat()
        auth.signIn("cy@orbit.app", "secret")
        val viewModel = settingsViewModel(recording())
        viewModel.onNameChange("Mine now")
        viewModel.save()

        assertTrue(writes.isEmpty())
    }

    @Test
    fun keepsWhatWasTypedAndSaysSoWhenSavingFailsUntilTheNextEdit() = runTest {
        flat()
        val offline = object : SpaceRepository by spaces {
            override suspend fun renameSpace(spaceId: String, name: String) =
                error("Could not reach the server")
        }
        val viewModel = settingsViewModel(offline)
        viewModel.onNameChange("Flat 5C")

        viewModel.save()

        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.saving)
        assertEquals("Flat 5C", viewModel.uiState.value.name)
        viewModel.onNameChange("Flat 5D")
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun anOwnerBacksOutOfDeletingAndThenDeletesTheSpace() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.confirmExit()
        assertTrue(viewModel.uiState.value.confirmingExit)
        viewModel.cancelExit()
        assertFalse(viewModel.uiState.value.confirmingExit)

        viewModel.confirmExit()
        viewModel.exit()

        assertTrue(viewModel.uiState.value.gone)
        assertTrue(spaces.observeSpaces().first().isEmpty())
    }

    @Test
    fun anAdminLeavesTheSpace() = runTest {
        flat()
        auth.signIn("ben@orbit.app", "secret")
        val viewModel = settingsViewModel()
        viewModel.confirmExit()
        viewModel.exit()

        assertTrue(viewModel.uiState.value.gone)
        auth.signIn("ana@orbit.app", "secret")
        assertEquals(listOf("ana@orbit.app", "cy@orbit.app"), spaceNow().memberIds.sorted())
    }

    @Test
    fun staysOpenAndSaysSoWhenDeletingFails() = runTest {
        flat()
        val failing = object : SpaceRepository by spaces {
            override suspend fun deleteSpace(spaceId: String) = error("Could not reach the server")
        }
        val viewModel = settingsViewModel(failing)
        viewModel.confirmExit()
        viewModel.exit()

        val state = viewModel.uiState.value
        assertTrue(state.confirmingExit)
        assertTrue(state.exitFailed)
        assertFalse(state.exiting)
        assertFalse(state.gone)
        viewModel.cancelExit()
        assertFalse(viewModel.uiState.value.exitFailed)
    }

    @Test
    fun exitingWithoutConfirmingDoesNothing() = runTest {
        flat()
        val viewModel = settingsViewModel()
        viewModel.exit()

        assertFalse(viewModel.uiState.value.gone)
        assertEquals(1, spaces.observeSpaces().first().size)
    }
}
