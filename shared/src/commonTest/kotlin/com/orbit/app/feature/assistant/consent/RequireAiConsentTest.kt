package com.orbit.app.feature.assistant.consent

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RequireAiConsentTest : ComposeTest() {
    private val auth = InMemoryAuthRepository().apply {
        runBlocking { register("ana@orbit.app", "secret", "Ana López") }
    }
    private val results = mutableListOf<Boolean>()

    private fun ComposeUiTest.show(repository: AuthRepository = auth) = setContent {
        OrbitTheme { RequireAiConsent(onResult = { results += it }, viewModel = AiConsentViewModel(repository)) }
    }

    @Test
    fun saysWhatIsSentAndThatNothingIsSaved() = runComposeUiTest {
        show()

        onNodeWithText("Before the assistant starts").assertIsDisplayed()
        onNodeWithText("Nothing is saved until you review the plan and confirm it.").assertIsDisplayed()
    }

    @Test
    fun acceptingOpensTheAssistantAndIsRemembered() = runComposeUiTest {
        show()

        onNodeWithText("Accept").performClick()

        waitForIdle()
        assertEquals(listOf(true), results)
        assertTrue(runBlocking { auth.session.first()!!.aiConsent })
    }

    @Test
    fun goingOnByHandIsTheManualPath() = runComposeUiTest {
        show()

        onNodeWithText("Set up by hand").performClick()

        waitForIdle()
        assertEquals(listOf(false), results)
    }

    @Test
    fun aUserWhoAcceptedBeforeIsNotAsked() = runComposeUiTest {
        runBlocking { auth.acceptAiConsent() }

        show()

        waitForIdle()
        assertEquals(listOf(true), results)
        onNodeWithText("Before the assistant starts").assertDoesNotExist()
    }

    @Test
    fun aFailedAcceptanceSaysSoAndStays() = runComposeUiTest {
        show(FailingAuthRepository(user = User("ana", "Ana López", photoUrl = null)))

        onNodeWithText("Accept").performClick()

        onNodeWithText("Could not save your answer. Try again.").assertIsDisplayed()
        onNodeWithText("Before the assistant starts").assertIsDisplayed()
        assertEquals(emptyList(), results)
    }
}
