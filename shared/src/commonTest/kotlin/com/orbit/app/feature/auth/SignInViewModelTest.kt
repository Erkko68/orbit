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
class SignInViewModelTest {
    private val auth = InMemoryAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signsInWithTheTypedEmail() = runTest {
        SignInViewModel(auth).signIn(" ana@orbit.app ", "secret")
        assertEquals("ana@orbit.app", auth.session.first()?.id)
    }

    @Test
    fun aBlankEmailSignsInThePlaceholderAccount() = runTest {
        SignInViewModel(auth).signIn("", "")
        assertEquals("you@orbit.app", auth.session.first()?.id)
    }
}
