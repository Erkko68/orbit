package com.orbit.app.feature.auth

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
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
import kotlin.test.assertNull
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
    fun anEmptyFormSignsNobodyInAndSaysWhatIsMissing() = runTest {
        val viewModel = SignInViewModel(auth)

        viewModel.signIn()

        assertNull(auth.session.first())
        assertEquals(
            listOf(CredentialsError.BlankEmail, CredentialsError.BlankPassword),
            viewModel.uiState.value.errors,
        )
    }

    @Test
    fun editingAFieldClearsTheErrors() {
        val viewModel = SignInViewModel(FailingAuthRepository())
        viewModel.signIn()
        assertTrue(viewModel.uiState.value.errors.isNotEmpty())

        viewModel.onEmailChange("ana@orbit.app")
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.signIn()
        viewModel.onPasswordChange("secret")
        assertTrue(viewModel.uiState.value.errors.isEmpty())
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
    fun saysWhyFirebaseRefusedAndClearsItOnTheNextEdit() {
        val viewModel = SignInViewModel(FailingAuthRepository(AuthException(AuthError.WRONG_CREDENTIALS)))
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("wrong")

        viewModel.signIn()

        assertEquals(AuthError.WRONG_CREDENTIALS, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)

        viewModel.onPasswordChange("secret")
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun anUnexpectedFailureIsAnUnknownError() {
        val viewModel = SignInViewModel(FailingAuthRepository())
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")

        viewModel.signIn()

        assertEquals(AuthError.UNKNOWN, viewModel.uiState.value.authError)
    }
}
