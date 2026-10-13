package com.orbit.app.feature.auth

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.testing.FailingAuthRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange(" ana@orbit.app ")
        viewModel.onPasswordChange("secret")

        viewModel.signIn()

        assertEquals("ana@orbit.app", auth.session.first()?.id)
        assertEquals(SignInUiState(email = " ana@orbit.app ", password = "secret"), viewModel.uiState.value)
    }

    @Test
    fun aBlankEmailSignsInThePlaceholderAccount() = runTest {
        SignInViewModel(auth).signIn()
        assertEquals("you@orbit.app", auth.session.first()?.id)
    }

    @Test
    fun togglesThePasswordVisibility() {
        val viewModel = SignInViewModel(auth)

        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.passwordVisible)

        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun aFailedSignInIsFlaggedUntilTheNextEdit() {
        val viewModel = SignInViewModel(FailingAuthRepository())

        viewModel.signIn()
        assertEquals(SignInUiState(failed = true), viewModel.uiState.value)

        viewModel.onPasswordChange("secret")
        assertFalse(viewModel.uiState.value.failed)

        viewModel.signIn()
        viewModel.onEmailChange("ana@orbit.app")
        assertFalse(viewModel.uiState.value.failed)
    }
}
