package com.orbit.app.feature.auth

import com.orbit.app.data.repository.InMemoryAuthRepository
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
class SessionViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun followsSignInAndSignOut() = runTest {
        val auth = InMemoryAuthRepository()
        val viewModel = SessionViewModel(auth)
        assertEquals(false, viewModel.signedIn.first { it != null })

        auth.signIn("ana@orbit.app", "secret")
        assertEquals(true, viewModel.signedIn.first { it == true })

        auth.signOut()
        assertEquals(false, viewModel.signedIn.first { it == false })
    }
}
