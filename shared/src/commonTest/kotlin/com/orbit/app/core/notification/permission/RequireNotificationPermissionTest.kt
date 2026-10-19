package com.orbit.app.core.notification.permission

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.FakeNotificationAccess
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RequireNotificationPermissionTest : ComposeTest() {
    private val access = FakeNotificationAccess()
    private val viewModel = NotificationPermissionViewModel(access)
    private val results = mutableListOf<Boolean>()

    private fun ComposeUiTest.show() = setContent {
        OrbitTheme {
            RequireNotificationPermission(onResult = { results += it }, viewModel = viewModel)
        }
    }

    /** What the system prompt answers when the user refuses: it is the platform's, so the test stands in for it. */
    private fun ComposeUiTest.refuseInThePrompt() {
        runOnIdle { viewModel.onAnswer(false) }
    }

    @Test
    fun reportsAllowedNotificationsWithoutAsking() = runComposeUiTest {
        access.allowed = true

        show()

        waitForIdle()
        assertEquals(listOf(true), results)
        onNodeWithText("Notifications are off").assertDoesNotExist()
    }

    @Test
    fun aRefusalShowsTheWayToTheSettings() = runComposeUiTest {
        show()
        refuseInThePrompt()

        onNodeWithText("Notifications are off").assertIsDisplayed()
        onNodeWithText("Open settings").performClick()

        assertEquals(1, access.settingsOpened)
        assertEquals(emptyList(), results)
    }

    @Test
    fun notNowReportsThemOff() = runComposeUiTest {
        show()
        refuseInThePrompt()

        onNodeWithText("Not now").performClick()

        waitForIdle()
        assertEquals(listOf(false), results)
        onNodeWithText("Notifications are off").assertDoesNotExist()
    }
}
