package com.orbit.app.feature.auth

import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.CompletableDeferred
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

    @Test
    fun signsInWithGoogle() = runTest {
        val viewModel = SignInViewModel(auth)

        viewModel.signInWithGoogle()

        assertEquals("google@orbit.app", auth.session.first()?.id)
        assertFalse(viewModel.uiState.value.submitting)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun saysWhyGoogleSignInFailed() {
        val viewModel = SignInViewModel(FailingAuthRepository(AuthException(AuthError.NETWORK)))

        viewModel.signInWithGoogle()

        assertEquals(AuthError.NETWORK, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun unexpectedFailureInGoogleSignInIsUnknownError() {
        val viewModel = SignInViewModel(FailingAuthRepository())

        viewModel.signInWithGoogle()

        assertEquals(AuthError.UNKNOWN, viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)
    }
    @Test
    fun opensTheResetDialogOnWhateverTheFormHas() {
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange(" ana@orbit.app ")

        viewModel.openPasswordReset()

        assertTrue(viewModel.uiState.value.resetOpen)
        assertEquals(" ana@orbit.app ", viewModel.uiState.value.resetEmail)
    }

    @Test
    fun sendsTheLinkAndSaysItWent() = runTest {
        val viewModel = SignInViewModel(auth)
        viewModel.openPasswordReset()
        viewModel.onResetEmailChange(" ana@orbit.app ")

        viewModel.sendPasswordReset()

        assertEquals(listOf("ana@orbit.app"), auth.passwordResets)
        val state = viewModel.uiState.value
        assertTrue(state.resetSent)
        assertFalse(state.resetSending)
        assertNull(state.resetError)
    }

    @Test
    fun sendsNothingToAnAddressThatIsNotOne() = runTest {
        val viewModel = SignInViewModel(auth)
        viewModel.openPasswordReset()
        viewModel.onResetEmailChange("ana")

        viewModel.sendPasswordReset()

        assertEquals(emptyList(), auth.passwordResets)
        assertEquals(listOf(CredentialsError.InvalidEmail), viewModel.uiState.value.resetErrors)
        assertFalse(viewModel.uiState.value.resetSent)
    }

    @Test
    fun saysWhenTheLinkCouldNotBeSent() = runTest {
        val viewModel = SignInViewModel(FailingAuthRepository(AuthException(AuthError.NETWORK)))
        viewModel.openPasswordReset()
        viewModel.onResetEmailChange("ana@orbit.app")

        viewModel.sendPasswordReset()

        val state = viewModel.uiState.value
        assertEquals(AuthError.NETWORK, state.resetError)
        assertFalse(state.resetSent)
        assertFalse(state.resetSending)
    }

    @Test
    fun editingTheAddressClearsWhatWasWrongWithIt() = runTest {
        val viewModel = SignInViewModel(auth)
        viewModel.openPasswordReset()
        viewModel.sendPasswordReset()
        assertEquals(listOf(CredentialsError.BlankEmail), viewModel.uiState.value.resetErrors)

        viewModel.onResetEmailChange("ana@orbit.app")

        assertEquals(emptyList(), viewModel.uiState.value.resetErrors)
        assertNull(viewModel.uiState.value.resetError)
    }

    @Test
    fun closesTheResetDialog() {
        val viewModel = SignInViewModel(auth)
        viewModel.openPasswordReset()

        viewModel.dismissPasswordReset()

        assertFalse(viewModel.uiState.value.resetOpen)
    }

    @Test
    fun whileOneLinkIsGoingNothingElseHappensToTheDialog() = runTest {
        val paused = PausedAuthRepository()
        val viewModel = SignInViewModel(paused)
        viewModel.openPasswordReset()
        viewModel.onResetEmailChange("ana@orbit.app")

        viewModel.sendPasswordReset()
        assertTrue(viewModel.uiState.value.resetSending)
        // A second tap on Send, and a tap outside, are both ignored until the first answer is back.
        viewModel.sendPasswordReset()
        viewModel.dismissPasswordReset()
        assertTrue(viewModel.uiState.value.resetOpen)

        paused.gate.complete(Unit)

        assertEquals(listOf("ana@orbit.app"), paused.passwordResets)
        assertTrue(viewModel.uiState.value.resetSent)
    }

    @Test
    fun doesNotOpenTheResetDialogWhileSigningIn() = runTest {
        val paused = PausedAuthRepository()
        val viewModel = SignInViewModel(paused)
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()
        assertTrue(viewModel.uiState.value.submitting)

        viewModel.openPasswordReset()

        assertFalse(viewModel.uiState.value.resetOpen)
        paused.gate.complete(Unit)
    }

}

/** Auth that holds every call until [gate] is completed, so a test can see the form mid-flight. */
private class PausedAuthRepository(
    private val inner: InMemoryAuthRepository = InMemoryAuthRepository(),
    val gate: CompletableDeferred<Unit> = CompletableDeferred(),
) : AuthRepository by inner {
    val passwordResets get() = inner.passwordResets

    override suspend fun signIn(email: String, password: String) {
        gate.await()
        inner.signIn(email, password)
    }

    override suspend fun sendPasswordReset(email: String) {
        gate.await()
        inner.sendPasswordReset(email)
    }
}
