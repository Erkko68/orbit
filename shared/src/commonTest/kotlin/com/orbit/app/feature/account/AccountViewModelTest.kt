package com.orbit.app.feature.account

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.CancellationException
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
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

    @Test
    fun loadsTheProfileAndWaitsForAChangeBeforeSaving() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana López")
        val user = auth.session.first()!!
        val state = observe(auth).uiState.value

        assertEquals("Ana López", state.displayName)
        assertEquals("AL", state.userInitials)
        assertEquals("en", state.language)
        assertEquals(user.timeZone.id, state.timeZone)
        assertTrue(state.canEdit)
        assertFalse(state.hasChanges)
        assertFalse(state.canSave)
    }

    @Test
    fun filtersTimeZonesAndChangesOnlyTheDraftAfterSelection() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        auth.updateProfile("Ana", "en", TimeZone.UTC)
        val original = auth.session.first()
        val viewModel = observe(auth)

        viewModel.openTimeZonePicker()
        assertTrue(viewModel.uiState.value.timeZonePickerOpen)
        assertEquals(TimeZone.UTC.id, viewModel.uiState.value.timeZoneOptions.first().value)
        assertTrue(viewModel.uiState.value.timeZoneOptions.first().selected)
        viewModel.onTimeZoneQueryChange(" rOmE ")
        assertEquals(listOf("Europe/Rome"), viewModel.uiState.value.timeZoneOptions.map { it.value })
        viewModel.onTimeZoneChange("Europe/Rome")

        assertFalse(viewModel.uiState.value.timeZonePickerOpen)
        assertEquals("", viewModel.uiState.value.timeZoneQuery)
        assertEquals("Europe/Rome", viewModel.uiState.value.timeZone)
        assertTrue(viewModel.uiState.value.canSave)
        assertEquals(original, auth.session.first())
    }

    @Test
    fun dismissingThePickerPreservesTheProfileAndSaveConfirmation() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val viewModel = observe(auth)
        viewModel.onDisplayNameChange("Ana Maria")
        viewModel.saveProfile()
        val saved = auth.session.first()
        val savedZone = viewModel.uiState.value.timeZone

        viewModel.openTimeZonePicker()
        viewModel.onTimeZoneQueryChange("no-such-zone")
        assertTrue(viewModel.uiState.value.timeZoneOptions.isEmpty())
        assertTrue(viewModel.uiState.value.saved)
        viewModel.closeTimeZonePicker()

        assertFalse(viewModel.uiState.value.timeZonePickerOpen)
        assertEquals("", viewModel.uiState.value.timeZoneQuery)
        assertEquals(savedZone, viewModel.uiState.value.timeZone)
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.canSave)
        assertEquals(saved, auth.session.first())
    }

    @Test
    fun editsStayInTheDraftUntilTheUserSaves() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val original = auth.session.first()!!
        val viewModel = observe(auth)

        viewModel.onDisplayNameChange("  Ana López  ")
        viewModel.selectLanguage("ca")
        viewModel.onTimeZoneChange("Europe/Madrid")

        assertEquals(original, auth.session.first())
        assertTrue(viewModel.uiState.value.canSave)
        viewModel.saveProfile()

        assertEquals(
            original.copy(displayName = "Ana López", language = "ca", timeZone = TimeZone.of("Europe/Madrid")),
            auth.session.first(),
        )
        val saved = viewModel.uiState.value
        assertEquals("Ana López", saved.displayName)
        assertEquals("AL", saved.userInitials)
        assertTrue(saved.saved)
        assertFalse(saved.saving)
        assertFalse(saved.canSave)

        viewModel.selectLanguage("es")
        assertFalse(viewModel.uiState.value.saved)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun invalidFieldsAndUnchangedValuesCannotBeSaved() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val original = auth.session.first()
        val viewModel = observe(auth)

        viewModel.saveProfile()
        viewModel.onDisplayNameChange("   ")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.saveProfile()

        viewModel.onDisplayNameChange("Ana López")
        viewModel.onTimeZoneChange("Not/AZone")
        assertTrue(viewModel.uiState.value.timeZoneInvalid)
        assertNull(viewModel.uiState.value.parsedTimeZone)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.saveProfile()

        viewModel.onTimeZoneChange("")
        assertFalse(viewModel.uiState.value.timeZoneInvalid)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.selectLanguage("xx")
        assertEquals("en", viewModel.uiState.value.language)
        assertEquals(original, auth.session.first())
    }

    @Test
    fun failedSaveKeepsTheDraftAndClearsItsErrorOnAnEdit() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val viewModel = observe(FailingAuthRepository(user = auth.session.first()))
        viewModel.onDisplayNameChange("Ana López")
        viewModel.selectLanguage("es")
        viewModel.onTimeZoneChange("Europe/Madrid")

        viewModel.saveProfile()

        val state = viewModel.uiState.value
        assertTrue(state.saveFailed)
        assertFalse(state.saving)
        assertTrue(state.canSave)
        assertEquals("Ana López", state.displayName)
        assertEquals("es", state.language)
        assertEquals("Europe/Madrid", state.timeZone)
        assertEquals("Ana", state.user?.displayName)
        assertFalse(state.saved)

        viewModel.onTimeZoneChange("UTC")
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun unavailableProfileUpdatesKeepTheDraftAndReportFailure() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val original = auth.session.first()
        val unsupported = object : AuthRepository by auth {
            override suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone) {
                throw UnsupportedOperationException("Profile persistence is pending")
            }
        }
        val viewModel = observe(unsupported)
        viewModel.onDisplayNameChange("Ana López")

        viewModel.saveProfile()

        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.saving)
        assertEquals("Ana López", viewModel.uiState.value.displayName)
        assertEquals(original, auth.session.first())
    }

    @Test
    fun signOutEndsTheSessionAndFailedSignOutAllowsRetry() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val failing = observe(FailingAuthRepository(user = auth.session.first()))

        failing.signOut()
        assertTrue(failing.uiState.value.signOutFailed)
        assertFalse(failing.uiState.value.signingOut)
        assertTrue(failing.uiState.value.canEdit)
        failing.signOut()
        assertTrue(failing.uiState.value.signOutFailed)
        failing.onDisplayNameChange("Ana López")
        assertFalse(failing.uiState.value.signOutFailed)

        val viewModel = observe(auth)
        viewModel.signOut()
        assertNull(auth.session.first())
        assertFalse(viewModel.uiState.value.canEdit)
        assertFalse(viewModel.uiState.value.signingOut)
    }

    @Test
    fun savingDisablesEditsAndPreventsDuplicateWritesOrLogout() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val gate = CompletableDeferred<Unit>()
        var writes = 0
        val delayed = object : AuthRepository by auth {
            override suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone) {
                writes++
                gate.await()
                auth.updateProfile(displayName, language, timeZone)
            }
        }
        val viewModel = observe(delayed)
        viewModel.onDisplayNameChange("Ana López")
        viewModel.saveProfile()

        assertTrue(viewModel.uiState.value.saving)
        assertFalse(viewModel.uiState.value.canEdit)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onDisplayNameChange("Other name")
        viewModel.saveProfile()
        viewModel.signOut()
        assertEquals(1, writes)
        assertEquals("Ana López", viewModel.uiState.value.displayName)
        assertEquals("Ana", auth.session.first()?.displayName)

        gate.complete(Unit)
        assertEquals("Ana López", auth.session.first()?.displayName)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun signingOutPreventsAnotherLogoutAndProfileWrites() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val delayed = object : AuthRepository by auth {
            override suspend fun signOut() {
                calls++
                gate.await()
                auth.signOut()
            }
        }
        val viewModel = observe(delayed)
        viewModel.onDisplayNameChange("Ana López")
        viewModel.signOut()

        assertTrue(viewModel.uiState.value.signingOut)
        assertFalse(viewModel.uiState.value.canEdit)
        viewModel.signOut()
        viewModel.saveProfile()
        assertEquals(1, calls)
        assertEquals("Ana", auth.session.first()?.displayName)

        gate.complete(Unit)
        assertNull(auth.session.first())
    }

    @Test
    fun repositoryUpdatesAreObservedWithoutOverwritingEditedFields() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val viewModel = observe(auth)
        viewModel.onDisplayNameChange("My draft")

        auth.updateProfile("Another name", "es", TimeZone.UTC)

        val state = viewModel.uiState.value
        assertEquals("My draft", state.displayName)
        assertEquals("es", state.language)
        assertEquals("UTC", state.timeZone)
        assertEquals("AN", state.userInitials)
    }

    @Test
    fun draftsCannotBeSavedIntoAnotherUsersAccount() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val viewModel = observe(auth)
        viewModel.onDisplayNameChange("Ana's draft")

        auth.register("ben@orbit.app", "secret", "Ben")

        assertEquals("Ben", viewModel.uiState.value.displayName)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.saveProfile()
        assertEquals("Ben", auth.session.first()?.displayName)
        viewModel.onDisplayNameChange("Ben Smith")
        viewModel.saveProfile()
        assertEquals("Ben Smith", auth.session.first()?.displayName)
    }

    @Test
    fun signedOutActionsDoNothing() = runTest {
        val state = AccountUiState()
        assertFalse(state.canEdit)
        assertFalse(state.canSave)
        assertFalse(state.hasChanges)
        assertEquals("", state.userInitials)
        val auth = InMemoryAuthRepository()
        val viewModel = observe(auth)

        viewModel.onDisplayNameChange("Ana")
        viewModel.selectLanguage("es")
        viewModel.onTimeZoneChange("UTC")
        viewModel.saveProfile()
        viewModel.signOut()

        assertEquals(AccountUiState(), viewModel.uiState.value)
        assertNull(auth.session.first())
    }

    @Test
    fun cancelledWritesAreNotReportedAsFailures() = runTest {
        val auth = InMemoryAuthRepository()
        auth.register("ana@orbit.app", "secret", "Ana")
        val cancelled = object : AuthRepository by auth {
            override suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone) {
                throw CancellationException("Screen closed")
            }

            override suspend fun signOut() {
                throw CancellationException("Screen closed")
            }
        }
        val saving = observe(cancelled)
        saving.onDisplayNameChange("Ana López")
        saving.saveProfile()
        assertFalse(saving.uiState.value.saveFailed)
        assertFalse(saving.uiState.value.saving)

        val signingOut = observe(cancelled)
        signingOut.signOut()
        assertFalse(signingOut.uiState.value.signOutFailed)
        assertFalse(signingOut.uiState.value.signingOut)
        assertEquals("Ana", auth.session.first()?.displayName)
    }
}
