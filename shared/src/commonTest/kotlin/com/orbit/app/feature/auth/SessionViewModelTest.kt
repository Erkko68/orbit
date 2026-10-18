package com.orbit.app.feature.auth

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryLastSpaceRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val lastSpace = InMemoryLastSpaceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sessionViewModel(spaces: SpaceRepository = this.spaces) = SessionViewModel(auth, spaces, lastSpace)

    private suspend fun SessionViewModel.started() = start.first { it != null }

    @Test
    fun followsSignInAndSignOut() = runTest {
        val viewModel = sessionViewModel()
        assertEquals(SessionStart.SignedOut, viewModel.started())

        auth.signIn("ana@orbit.app", "secret")
        assertEquals(SessionStart.Spaces, viewModel.start.first { it == SessionStart.Spaces })

        auth.signOut()
        assertEquals(SessionStart.SignedOut, viewModel.start.first { it == SessionStart.SignedOut })
    }

    @Test
    fun startsInTheListWhenNoSpaceWasOpened() = runTest {
        auth.signIn("ana@orbit.app", "secret")

        assertEquals(SessionStart.Spaces, sessionViewModel().started())
    }

    @Test
    fun startsInTheLastSpaceWhenTheUserStillBelongsToIt() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        lastSpace.remember(id)

        assertEquals(SessionStart.Space(id), sessionViewModel().started())
    }

    @Test
    fun fallsBackToTheListAndForgetsASpaceTheUserNoLongerBelongsTo() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Flat 4B", accent = 0)
        lastSpace.remember("a-space-ana-left")

        assertEquals(SessionStart.Spaces, sessionViewModel().started())
        assertEquals(null, lastSpace.lastSpaceId())
    }

    @Test
    fun staysSignedOutWhateverWasOpenedLast() = runTest {
        lastSpace.remember("space-1")

        assertEquals(SessionStart.SignedOut, sessionViewModel().started())
    }

    @Test
    fun startsInTheListButKeepsTheMemoryWhenTheSpacesDoNotArrive() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        lastSpace.remember("space-1")

        assertEquals(SessionStart.Spaces, sessionViewModel(NeverLoadedSpaces).started())
        assertEquals("space-1", lastSpace.lastSpaceId())
    }

    @Test
    fun startsInTheListButKeepsTheMemoryWhenTheSpacesFailToLoad() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        lastSpace.remember("space-1")

        assertEquals(SessionStart.Spaces, sessionViewModel(FlakySpaceRepository(failures = 1)).started())
        assertEquals("space-1", lastSpace.lastSpaceId())
    }

    /** Offline with nothing cached: the spaces never answer. */
    private object NeverLoadedSpaces : SpaceRepository {
        override fun observeSpaces(): Flow<List<Space>> = flow { awaitCancellation() }

        override fun observeMembers(spaceId: String): Flow<List<Member>> = flow { awaitCancellation() }

        override suspend fun createSpace(name: String, accent: Int): String = error("Not used by these tests")
    }
}
