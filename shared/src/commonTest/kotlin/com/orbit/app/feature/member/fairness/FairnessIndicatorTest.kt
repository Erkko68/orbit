package com.orbit.app.feature.member.fairness

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.usecase.EffortShare
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FairnessIndicatorTest : ComposeTest() {
    @Test
    fun noContributionsSaysNoDataInsteadOfAnUnfairShare() = runComposeUiTest {
        setContent { OrbitTheme { FairnessStatus(FairnessUiState(loading = false), {}) } }
        onNodeWithText("No effort points recorded in the last 30 days.").assertIsDisplayed()
    }

    @Test
    fun failedReadOffersRetry() = runComposeUiTest {
        var retries = 0
        setContent { OrbitTheme { FairnessStatus(FairnessUiState(historyFailed = true), { retries++ }) } }
        onNodeWithText("Could not load effort").assertIsDisplayed()
        onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun showsPointsAndComparisonWithAnExplicitRoundedLabel() = runComposeUiTest {
        setContent { OrbitTheme { MemberEffort(EffortShare("a", 2, 3, 3)) } }
        onNodeWithText("67% · equal share 33%").assertIsDisplayed()
        onNodeWithText("2 of 3 points · rounded percentages").assertIsDisplayed()
    }
}
