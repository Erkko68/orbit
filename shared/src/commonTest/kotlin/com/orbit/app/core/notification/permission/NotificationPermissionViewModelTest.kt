package com.orbit.app.core.notification.permission

import com.orbit.app.testing.FakeNotificationAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPermissionViewModelTest {
    private val access = FakeNotificationAccess()
    private val viewModel by lazy { NotificationPermissionViewModel(access) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun allowedNotificationsNeedNoPrompt() {
        access.allowed = true

        viewModel.onResume()

        assertEquals(NotificationPermissionUiState(result = true), viewModel.uiState.value)
    }

    @Test
    fun asksThroughTheSystemPromptWhenNotificationsAreOff() {
        viewModel.onResume()

        assertTrue(viewModel.uiState.value.prompting)
    }

    @Test
    fun theUserAllowsNotificationsInThePrompt() {
        viewModel.onResume()
        viewModel.onPromptShown()

        viewModel.onAnswer(true)

        assertEquals(NotificationPermissionUiState(result = true), viewModel.uiState.value)
    }

    @Test
    fun aRefusalOffersTheSettingsInsteadOfAskingAgain() {
        viewModel.onResume()
        viewModel.onPromptShown()
        viewModel.onAnswer(false)

        viewModel.onResume()

        assertEquals(NotificationPermissionUiState(refused = true), viewModel.uiState.value)
    }

    @Test
    fun comingBackFromThePromptChecksNothingUntilItAnswers() {
        viewModel.onResume()
        viewModel.onPromptShown()
        access.allowed = true

        viewModel.onResume()

        assertEquals(NotificationPermissionUiState(), viewModel.uiState.value)
    }

    @Test
    fun opensTheSystemSettings() {
        viewModel.openSettings()

        assertEquals(1, access.settingsOpened)
    }

    @Test
    fun comingBackFromTheSettingsWithNotificationsOnCountsAsAllowed() {
        refuse()
        viewModel.openSettings()
        access.allowed = true

        viewModel.onResume()

        assertEquals(NotificationPermissionUiState(result = true), viewModel.uiState.value)
    }

    @Test
    fun notNowGivesUp() {
        refuse()

        viewModel.dismiss()

        assertEquals(NotificationPermissionUiState(result = false), viewModel.uiState.value)
    }

    @Test
    fun theNextRequestStartsOver() {
        refuse()
        viewModel.dismiss()

        viewModel.onResultDelivered()
        viewModel.onResume()

        val state = viewModel.uiState.value
        assertNull(state.result)
        assertFalse(state.refused)
        assertTrue(state.prompting)
    }

    private fun refuse() {
        viewModel.onResume()
        viewModel.onPromptShown()
        viewModel.onAnswer(false)
    }
}
