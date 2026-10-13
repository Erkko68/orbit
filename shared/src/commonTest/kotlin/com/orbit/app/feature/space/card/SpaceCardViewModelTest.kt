package com.orbit.app.feature.space.card

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceCardViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsItsSpace() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Garage", accent = 1)
        val id = spaces.createSpace("Flat 4B", accent = 2)

        val state = SpaceCardViewModel(id, spaces).uiState.first { it.name.isNotEmpty() }

        assertEquals(SpaceCardUiState(name = "Flat 4B", accent = 2, memberCount = 1), state)
    }

    @Test
    fun staysEmptyForASpaceItCannotSee() = runTest {
        val viewModel = SpaceCardViewModel("missing", spaces)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(SpaceCardUiState(), viewModel.uiState.value)
    }
}
