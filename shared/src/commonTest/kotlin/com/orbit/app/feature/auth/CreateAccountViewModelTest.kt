package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
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
class CreateAccountViewModelTest {
    private val auth = InMemoryAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun CreateAccountViewModel.fill(email: String, password: String, confirmation: String) {
        onEmailChange(email)
        onPasswordChange(password)
        onConfirmationChange(confirmation)
    }

    @Test
    fun registersAndSignsInTheNewUserNamedAfterTheirEmail() = runTest {
        val viewModel = CreateAccountViewModel(auth)
        viewModel.fill(" ana@orbit.app ", "12345678", "12345678")

        viewModel.createAccount()

        val user = auth.session.first()
        assertEquals("ana@orbit.app", user?.id)
        assertEquals("ana", user?.displayName)
        assertFalse(viewModel.uiState.value.submitting)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun anInvalidFormRegistersNobodyAndSaysEverythingThatIsWrong() = runTest {
        val viewModel = CreateAccountViewModel(auth)
        viewModel.fill("ana", "123", "456")

        viewModel.createAccount()

        assertNull(auth.session.first())
        assertEquals(
            listOf(CredentialsError.InvalidEmail, CredentialsError.ShortPassword, CredentialsError.PasswordsDiffer),
            viewModel.uiState.value.errors,
        )
    }

    @Test
    fun editingAFieldClearsTheErrors() {
        val viewModel = CreateAccountViewModel(auth)
        viewModel.createAccount()
        assertTrue(viewModel.uiState.value.errors.isNotEmpty())

        viewModel.onEmailChange("ana@orbit.app")
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.createAccount()
        viewModel.onPasswordChange("12345678")
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.createAccount()
        viewModel.onConfirmationChange("12345678")
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun togglesTheVisibilityOfEachPasswordField() {
        val viewModel = CreateAccountViewModel(auth)

        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.passwordVisible)
        assertFalse(viewModel.uiState.value.confirmationVisible)

        viewModel.toggleConfirmationVisibility()
        assertTrue(viewModel.uiState.value.confirmationVisible)

        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.passwordVisible)
    }

    @Test
    fun saysWhyFirebaseRefusedAndClearsItOnTheNextEdit() {
        val viewModel = CreateAccountViewModel(FailingAuthRepository(AuthException(AuthError.EMAIL_IN_USE)))
        viewModel.fill("ana@orbit.app", "12345678", "12345678")

        viewModel.createAccount()

        assertEquals(AuthError.EMAIL_IN_USE, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)

        viewModel.onEmailChange("ben@orbit.app")
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun anUnexpectedFailureIsAnUnknownError() {
        val viewModel = CreateAccountViewModel(FailingAuthRepository())
        viewModel.fill("ana@orbit.app", "12345678", "12345678")

        viewModel.createAccount()

        assertEquals(AuthError.UNKNOWN, viewModel.uiState.value.authError)
    }

    @Test
    fun signsInWithGoogle() = runTest {
        val viewModel = CreateAccountViewModel(auth)

        viewModel.signInWithGoogle()

        assertEquals("google@orbit.app", auth.session.first()?.id)
        assertFalse(viewModel.uiState.value.submitting)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun saysWhyGoogleSignInFailed() {
        val viewModel = CreateAccountViewModel(FailingAuthRepository(AuthException(AuthError.NETWORK)))

        viewModel.signInWithGoogle()

        assertEquals(AuthError.NETWORK, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun unexpectedFailureInGoogleSignInIsUnknownError() {
        val viewModel = CreateAccountViewModel(FailingAuthRepository())

        viewModel.signInWithGoogle()

        assertEquals(AuthError.UNKNOWN, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)
    }
}
