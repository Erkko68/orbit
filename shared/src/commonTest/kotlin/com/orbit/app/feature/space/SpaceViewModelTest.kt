package com.orbit.app.feature.space

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createsASpaceForTheSignedInUser() = runTest {
        val auth = InMemoryAuthRepository()
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = SpaceViewModel(InMemorySpaceRepository(auth))

        viewModel.createSpace("  Flat 4B ")

        val spaces = viewModel.uiState.first { it.spaces.isNotEmpty() }.spaces
        assertEquals(listOf("Flat 4B"), spaces.map { it.name })
    }
}
