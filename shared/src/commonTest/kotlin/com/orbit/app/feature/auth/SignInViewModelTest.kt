package com.orbit.app.feature.auth

import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.domain.model.AccountLinkException
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.domain.model.LinkProof
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
import kotlin.test.assertNotNull
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
    fun offersToLinkWhenTheAddressSignsInWithGoogle() {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.GOOGLE))
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")

        viewModel.signIn()

        assertEquals(AccountLinkUiState("ana@orbit.app", LinkProof.GOOGLE), viewModel.uiState.value.link)
        assertNull(viewModel.uiState.value.authError)
        assertFalse(viewModel.uiState.value.submitting)
    }

    @Test
    fun settingAPasswordOnAGoogleAccountKeepsThatOneAccount() = runTest {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.GOOGLE))
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()

        viewModel.onLinkPasswordChange("a-new-secret")
        viewModel.confirmLink()

        assertEquals(listOf(LinkProof.PASSWORD), auth.linked)
        assertEquals("ana@orbit.app", auth.session.first()?.id)
        assertNull(viewModel.uiState.value.link)
    }

    @Test
    fun offersToLinkWhenTheGoogleAddressAlreadyHasAPassword() {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.PASSWORD))
        val viewModel = SignInViewModel(auth)

        viewModel.signInWithGoogle()

        assertEquals(AccountLinkUiState("ana@orbit.app", LinkProof.PASSWORD), viewModel.uiState.value.link)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun thePasswordProvesTheAccountAndLinksGoogleToIt() = runTest {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.PASSWORD))
        val viewModel = SignInViewModel(auth)
        viewModel.signInWithGoogle()

        viewModel.onLinkPasswordChange("secret")
        viewModel.confirmLink()

        assertEquals(listOf(LinkProof.GOOGLE), auth.linked)
        // The account they had, not the Google one a second sign-in would have made.
        assertEquals("ana@orbit.app", auth.session.first()?.id)
        assertNull(viewModel.uiState.value.link)
    }

    @Test
    fun linksNothingWithoutAPassword() = runTest {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.PASSWORD))
        val viewModel = SignInViewModel(auth)
        viewModel.signInWithGoogle()

        viewModel.confirmLink()

        assertTrue(auth.linked.isEmpty())
        assertNull(auth.session.first())
        assertFalse(viewModel.uiState.value.link?.canLink == true)
    }

    @Test
    fun saysWhyTheLinkFailedAndClearsItOnTheNextEdit() {
        val auth = InMemoryAuthRepository(
            collision = AccountLinkException("ana@orbit.app", LinkProof.PASSWORD),
            linkFailure = AuthException(AuthError.WRONG_CREDENTIALS),
        )
        val viewModel = SignInViewModel(auth)
        viewModel.signInWithGoogle()
        viewModel.onLinkPasswordChange("wrong")

        viewModel.confirmLink()

        assertEquals(AuthError.WRONG_CREDENTIALS, viewModel.uiState.value.link?.failed)
        assertFalse(viewModel.uiState.value.link?.linking == true)

        viewModel.onLinkPasswordChange("secret")
        assertNull(viewModel.uiState.value.link?.failed)
    }

    @Test
    fun aDifferentGoogleAccountIsItsOwnMessage() {
        val auth = InMemoryAuthRepository(
            collision = AccountLinkException("ana@orbit.app", LinkProof.GOOGLE),
            linkFailure = AuthException(AuthError.DIFFERENT_GOOGLE_ACCOUNT),
        )
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()
        viewModel.onLinkPasswordChange("a-new-secret")

        viewModel.confirmLink()

        assertEquals(AuthError.DIFFERENT_GOOGLE_ACCOUNT, viewModel.uiState.value.link?.failed)
        assertNotNull(viewModel.uiState.value.link?.passwordError)
    }

    @Test
    fun anUnexpectedLinkFailureIsAnUnknownError() {
        val auth = InMemoryAuthRepository(
            collision = AccountLinkException("ana@orbit.app", LinkProof.PASSWORD),
            linkFailure = IllegalStateException("No connection"),
        )
        val viewModel = SignInViewModel(auth)
        viewModel.signInWithGoogle()
        viewModel.onLinkPasswordChange("secret")

        viewModel.confirmLink()

        assertEquals(AuthError.UNKNOWN, viewModel.uiState.value.link?.failed)
    }

    @Test
    fun dismissingTheOfferLeavesTheFormAsItWas() {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.GOOGLE))
        val viewModel = SignInViewModel(auth)
        viewModel.onEmailChange("ana@orbit.app")
        viewModel.onPasswordChange("secret")
        viewModel.signIn()

        viewModel.dismissLink()

        assertNull(viewModel.uiState.value.link)
        assertEquals("ana@orbit.app", viewModel.uiState.value.email)
        assertTrue(auth.linked.isEmpty())
    }

    @Test
    fun togglesTheLinkPasswordVisibility() {
        val auth = InMemoryAuthRepository(AccountLinkException("ana@orbit.app", LinkProof.PASSWORD))
        val viewModel = SignInViewModel(auth)
        viewModel.signInWithGoogle()

        viewModel.toggleLinkPasswordVisibility()
        assertTrue(viewModel.uiState.value.link?.passwordVisible == true)

        viewModel.toggleLinkPasswordVisibility()
        assertFalse(viewModel.uiState.value.link?.passwordVisible == true)
    }
}
