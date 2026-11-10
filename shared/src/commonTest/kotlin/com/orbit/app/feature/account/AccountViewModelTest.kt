package com.orbit.app.feature.account

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.domain.model.User
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
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
    private val ana = User("ana", "Ana López", photoUrl = null, email = "ana@orbit.app")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.observe(auth: AuthRepository): AccountViewModel =
        observe(auth, InMemorySpaceRepository(auth))

    private fun TestScope.observe(auth: AuthRepository, spaces: SpaceRepository): AccountViewModel {
        val viewModel = AccountViewModel(auth, spaces)
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
        assertEquals(AccountSheetKind.EDIT_NAME, viewModel.uiState.value.dialog)
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
        assertEquals(AccountSheetKind.EDIT_NAME, failed.dialog)
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
            assertEquals(AccountSheetKind.CHANGE_PASSWORD, viewModel.uiState.value.dialog)
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
        assertEquals(AccountSheetKind.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        viewModel.onDeletePasswordChange("secret")
        viewModel.deleteAccount()

        assertNull(auth.session.first())
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun aFailedDeleteStaysInTheDialog() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))
        viewModel.openDeleteAccount()

        viewModel.deleteAccount()

        assertEquals(AccountSheetKind.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        assertEquals(Res.string.error_unknown, viewModel.uiState.value.dialogError)
    }

    @Test
    fun aWriteInProgressRunsOnceAndKeepsItsDialogOpen() = runTest {
        val auth = signedIn()
        val gate = CompletableDeferred<Unit>()
        var deletes = 0
        val slow = object : AuthRepository by auth {
            override suspend fun deleteAccount(currentPassword: String?) {
                deletes++
                gate.await()
                auth.deleteAccount(currentPassword)
            }
        }
        val viewModel = observe(slow)
        viewModel.openDeleteAccount()
        viewModel.onDeletePasswordChange("secret")

        viewModel.deleteAccount()
        viewModel.deleteAccount()
        viewModel.dismissDialog()
        viewModel.signOut()

        assertTrue(viewModel.uiState.value.submitting)
        assertEquals(AccountSheetKind.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
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
    fun signsOutOnceConfirmed() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)

        viewModel.openSignOut()
        assertEquals(AccountSheetKind.SIGN_OUT, viewModel.uiState.value.dialog)
        viewModel.signOut()

        assertNull(auth.session.first())
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun aFailedSignOutStaysInTheDialog() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))
        viewModel.openSignOut()

        viewModel.signOut()

        assertEquals(AccountSheetKind.SIGN_OUT, viewModel.uiState.value.dialog)
        assertEquals(Res.string.error_unknown, viewModel.uiState.value.dialogError)
        assertFalse(viewModel.uiState.value.submitting)
    }
    @Test
    fun deletingWithoutThePasswordDeletesNothing() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)
        viewModel.openDeleteAccount()

        viewModel.deleteAccount()

        assertFalse(viewModel.uiState.value.submitting)
        assertEquals(AccountSheetKind.DELETE_ACCOUNT, viewModel.uiState.value.dialog)
        assertEquals(Res.string.error_current_password_wrong, viewModel.uiState.value.deletePasswordError)
        assertEquals("ana@orbit.app", auth.session.first()?.id)
    }

    @Test
    fun aWrongPasswordShowsUnderTheFieldItWasTypedIn() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)
        viewModel.openDeleteAccount()
        viewModel.onDeletePasswordChange("not-it")

        viewModel.deleteAccount()

        assertEquals(Res.string.error_current_password_wrong, viewModel.uiState.value.deletePasswordError)
        assertNull(viewModel.uiState.value.dialogError)
        assertEquals("ana@orbit.app", auth.session.first()?.id)
    }

    @Test
    fun anAccountThatOnlySignsInWithGoogleHasNoPasswordToGive() = runTest {
        val auth = InMemoryAuthRepository().apply { signInWithGoogle() }
        val viewModel = observe(auth)
        assertFalse(viewModel.uiState.value.hasPassword)

        viewModel.openDeleteAccount()
        assertTrue(viewModel.uiState.value.canDelete)
        viewModel.deleteAccount()

        assertNull(auth.session.first())
    }

    @Test
    fun deletesTheSpacesWhereTheUserIsAloneAndLeavesTheRest() = runTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        auth.register("bob@orbit.app", "secret", "Bob")
        val shared = spaces.createSpace("Flat 4B", accent = 0)
        auth.register("ana@orbit.app", "secret", "Ana López")
        spaces.addMember(shared, Member("ana@orbit.app", "Ana López", photoUrl = null, role = Role.MEMBER))
        val alone = spaces.createSpace("Studio", accent = 1)
        val viewModel = observe(auth, spaces)
        viewModel.openDeleteAccount()
        viewModel.onDeletePasswordChange("secret")

        viewModel.deleteAccount()

        assertNull(auth.session.first())
        assertEquals(emptyList(), spaces.observeMembers(alone).first())
        assertEquals(listOf("bob@orbit.app"), spaces.observeMembers(shared).first().map { it.userId })
    }

    @Test
    fun willNotDeleteWhileASpaceWithOtherPeopleInItIsOwned() = runTest {
        val auth = signedIn()
        val spaces = InMemorySpaceRepository(auth)
        val owned = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(owned, Member("bob@orbit.app", "Bob", photoUrl = null, role = Role.MEMBER))
        val viewModel = observe(auth, spaces)
        viewModel.openDeleteAccount()
        viewModel.onDeletePasswordChange("secret")

        viewModel.deleteAccount()

        assertEquals(listOf("Flat 4B"), viewModel.uiState.value.handOver)
        assertFalse(viewModel.uiState.value.submitting)
        assertEquals("ana@orbit.app", auth.session.first()?.id)
        assertEquals(2, spaces.observeMembers(owned).first().size)
    }

    @Test
    fun typingAgainClearsTheSpacesToHandOver() = runTest {
        val auth = signedIn()
        val spaces = InMemorySpaceRepository(auth)
        val owned = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(owned, Member("bob@orbit.app", "Bob", photoUrl = null, role = Role.MEMBER))
        val viewModel = observe(auth, spaces)
        viewModel.openDeleteAccount()
        viewModel.onDeletePasswordChange("secret")
        viewModel.deleteAccount()
        assertTrue(viewModel.uiState.value.handOver.isNotEmpty())

        viewModel.onDeletePasswordChange("secret again")

        assertEquals(emptyList(), viewModel.uiState.value.handOver)
    }

}
