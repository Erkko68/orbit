package com.orbit.app.feature.assistant.consent

import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiConsentViewModelTest {
    private val ana = User("ana", "Ana López", photoUrl = null)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.observe(auth: AuthRepository): AiConsentViewModel {
        val viewModel = AiConsentViewModel(auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun signedIn() =
        InMemoryAuthRepository().apply { register("ana@orbit.app", "secret", "Ana López") }

    @Test
    fun asksAUserWhoHasNotAccepted() = runTest {
        val state = observe(signedIn()).uiState.value

        assertTrue(state.asking)
        assertNull(state.result)
    }

    @Test
    fun doesNotAskAgainOnceAccepted() = runTest {
        val auth = signedIn().apply { acceptAiConsent() }

        val state = observe(auth).uiState.value

        assertFalse(state.asking)
        assertEquals(true, state.result)
    }

    @Test
    fun asksNothingWhileSignedOut() = runTest {
        val state = observe(InMemoryAuthRepository()).uiState.value

        assertFalse(state.asking)
        assertNull(state.result)
    }

    @Test
    fun acceptingSavesItToTheProfile() = runTest {
        val auth = signedIn()
        val viewModel = observe(auth)

        viewModel.accept()

        assertEquals(true, viewModel.uiState.value.result)
        assertTrue(auth.session.first()!!.aiConsent)
    }

    @Test
    fun aFailedAcceptanceStaysInTheNoticeUntilTheNextAttempt() = runTest {
        val viewModel = observe(FailingAuthRepository(user = ana))

        viewModel.accept()

        val failed = viewModel.uiState.value
        assertTrue(failed.asking)
        assertTrue(failed.acceptFailed)
        assertFalse(failed.submitting)
        assertNull(failed.result)
    }

    @Test
    fun anAcceptanceBeingSavedCannotRunTwiceOrBeDeclined() = runTest {
        val gate = CompletableDeferred<Unit>()
        var saves = 0
        val auth = signedIn()
        val slow = object : AuthRepository by auth {
            override suspend fun acceptAiConsent() {
                saves++
                gate.await()
                auth.acceptAiConsent()
            }
        }
        val viewModel = observe(slow)

        viewModel.accept()
        viewModel.accept()
        viewModel.decline()

        assertTrue(viewModel.uiState.value.submitting)
        assertEquals(1, saves)

        gate.complete(Unit)
        assertEquals(true, viewModel.uiState.value.result)
    }

    @Test
    fun decliningGoesOnByHandAndTheNextOpeningAsksAgain() = runTest {
        val viewModel = observe(signedIn())

        viewModel.decline()
        assertEquals(false, viewModel.uiState.value.result)

        viewModel.onResultDelivered()
        assertTrue(viewModel.uiState.value.asking)
    }
}
