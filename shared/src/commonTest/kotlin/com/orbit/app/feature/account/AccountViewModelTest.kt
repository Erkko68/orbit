package com.orbit.app.feature.account

import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.error_current_password_wrong
import orbit.shared.generated.resources.error_network
import orbit.shared.generated.resources.error_unknown
import orbit.shared.generated.resources.error_weak_password
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val ana = User("ana", "Ana López", photoUrl = null, "en", TimeZone.UTC, email = "ana@orbit.app")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.observe(auth: AuthRepository): AccountViewModel {
        val viewModel = AccountViewModel(auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun signedIn() =
        InMemoryAuthRepository().apply { register("ana@orbit.app", "secret", "Ana López") }

    @Test
    fun showsTheSignedInUser() = runTest {
        val state = observe(signedIn()).uiState.value

        assertEquals("Ana López", state.name)
        assertEquals("ana@orbit.app", state.email)
        assertEquals("AL", state.initials)
        assertNull(state.dialog)
    }

    @Test
    fun renamesOnceTheNameChanges() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)

        viewModel.openEditName()
        assertEquals(AccountDialog.EDIT_NAME, viewModel.uiState.value.dialog)
        assertEquals("Ana López", viewModel.uiState.value.nameDraft)
        assertFalse(viewModel.uiState.value.canSaveName)

        viewModel.onNameChange("  Ana  ")
        viewModel.saveName()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals("Ana", auth.session.first()?.displayName)
        assertEquals("Ana", viewModel.uiState.value.name)
    }

    @Test
    fun aFailedRenameStaysInTheDialogUntilTheNextEdit() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))
        viewModel.openEditName()
        viewModel.onNameChange("Ana")

        viewModel.saveName()

        val failed = viewModel.uiState.value
        assertEquals(AccountDialog.EDIT_NAME, failed.dialog)
        assertFalse(failed.submitting)
        assertEquals(Res.string.error_unknown, failed.dialogError)

        viewModel.onNameChange("Ana L")
        assertNull(viewModel.uiState.value.dialogError)
    }

    @Test
    fun checksThePasswordsBeforeChangingThem() = runTest {
        val viewModel = observe(signedIn())
        viewModel.openChangePassword()
        viewModel.onNewPasswordChange("short")
        viewModel.onConfirmationChange("other")

        viewModel.changePassword()

        val state = viewModel.uiState.value
        assertEquals(
            listOf(CredentialsError.BlankPassword, CredentialsError.ShortPassword, CredentialsError.PasswordsDiffer),
            state.passwordErrors,
        )
        assertTrue(state.currentPasswordError != null)
        assertTrue(state.newPasswordError != null)
        assertTrue(state.confirmationError != null)

        viewModel.onCurrentPasswordChange("s")
        assertEquals(emptyList(), viewModel.uiState.value.passwordErrors)
    }

    @Test
    fun aWrongCurrentPasswordShowsUnderItThenTheRightOneClosesTheDialog() = runTest {
        val viewModel = observe(signedIn())
        viewModel.openChangePassword()
        viewModel.onCurrentPasswordChange("wrong")
        viewModel.onNewPasswordChange("new-secret")
        viewModel.onConfirmationChange("new-secret")

        viewModel.changePassword()

        assertEquals(Res.string.error_current_password_wrong, viewModel.uiState.value.currentPasswordError)
        assertNull(viewModel.uiState.value.confirmationError)

        viewModel.onCurrentPasswordChange("secret")
        viewModel.changePassword()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals("", viewModel.uiState.value.newPassword)
    }

    @Test
    fun aRefusedNewPasswordShowsUnderItAndOtherFailuresUnderTheLastField() = runTest {
        val weak = observe(FailingAuthRepository(AuthException(AuthError.WEAK_PASSWORD), user = ana))
        val offline = observe(FailingAuthRepository(AuthException(AuthError.NETWORK), user = ana))
        for (viewModel in listOf(weak, offline)) {
            viewModel.openChangePassword()
            viewModel.onCurrentPasswordChange("secret")
            viewModel.onNewPasswordChange("new-secret")
            viewModel.onConfirmationChange("new-secret")
            viewModel.changePassword()
            assertEquals(AccountDialog.CHANGE_PASSWORD, viewModel.uiState.value.dialog)
        }

        assertEquals(Res.string.error_weak_password, weak.uiState.value.newPasswordError)
        assertNull(weak.uiState.value.confirmationError)
        assertEquals(Res.string.error_network, offline.uiState.value.confirmationError)
        assertNull(offline.uiState.value.newPasswordError)
    }

    @Test
    fun togglesThePasswordsTogether() = runTest {
        val viewModel = observe(signedIn())
        viewModel.openChangePassword()

        viewModel.togglePasswordsVisibility()
        assertTrue(viewModel.uiState.value.passwordsVisible)

        viewModel.togglePasswordsVisibility()
        assertFalse(viewModel.uiState.value.passwordsVisible)
    }

    @Test
    fun deletingTheAccountSignsOut() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)

        viewModel.openDeleteAccount()
        assertEquals(AccountDialog.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        viewModel.deleteAccount()

        assertNull(auth.session.first())
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun aFailedDeleteStaysInTheDialog() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))
        viewModel.openDeleteAccount()

        viewModel.deleteAccount()

        assertEquals(AccountDialog.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        assertEquals(Res.string.error_unknown, viewModel.uiState.value.dialogError)
    }

    @Test
    fun aWriteInProgressRunsOnceAndKeepsItsDialogOpen() = runTest {
        val auth = signedIn()
        val gate = CompletableDeferred<Unit>()
        var deletes = 0
        val slow = object : AuthRepository by auth {
            override suspend fun deleteAccount() {
                deletes++
                gate.await()
                auth.deleteAccount()
            }
        }
        val viewModel = observe(slow)
        viewModel.openDeleteAccount()

        viewModel.deleteAccount()
        viewModel.deleteAccount()
        viewModel.dismissDialog()
        viewModel.signOut()

        assertTrue(viewModel.uiState.value.submitting)
        assertEquals(AccountDialog.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        gate.complete(Unit)
        assertEquals(1, deletes)
        assertNull(auth.session.first())
    }

    @Test
    fun dismissingForgetsWhatWasTyped() = runTest {
        val viewModel = observe(signedIn())
        viewModel.openChangePassword()
        viewModel.onCurrentPasswordChange("secret")

        viewModel.dismissDialog()

        assertNull(viewModel.uiState.value.dialog)
        assertEquals("", viewModel.uiState.value.currentPassword)
    }

    @Test
    fun signsOut() = runTest {
        val auth = signedIn()

        observe(auth).signOut()

        assertNull(auth.session.first())
    }

    @Test
    fun aFailedSignOutIsFlaggedUntilTheNextAttempt() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))

        viewModel.signOut()

        assertTrue(viewModel.uiState.value.signOutFailed)
        assertFalse(viewModel.uiState.value.submitting)
    }
}
