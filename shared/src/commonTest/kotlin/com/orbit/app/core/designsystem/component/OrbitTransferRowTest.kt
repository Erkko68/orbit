package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_wallet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitTransferRowTest : ComposeTest() {
    private val faces = listOf(
        OrbitFace("bea", "Bea", "B", Color.Red, OrbitShape.Circle, null),
        OrbitFace("ana", "Ana", "A", Color.Blue, OrbitShape.Circle, null),
    )

    @Test
    fun onlyThePaidActionAsksAndItSitsAfterTheAmount() = runComposeUiTest {
        var asked = 0
        setContent {
            OrbitTheme {
                OrbitTransferRow(
                    faces, "You owe Bea", "12,40 €", Modifier.width(360.dp),
                    action = "Pay", actionIcon = Res.drawable.ic_wallet, actionDescription = "Mark as paid: You owe Bea",
                    onPaid = { asked++ },
                )
            }
        }
        val sentence = onNodeWithText("You owe Bea", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val amount = onNodeWithText("12,40 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val action = onNodeWithContentDescription("Mark as paid: You owe Bea").fetchSemanticsNode().boundsInRoot
        assertTrue(sentence.bottom <= amount.top)
        assertTrue(amount.right <= action.left)
        onNodeWithText("You owe Bea").assertHasNoClickAction()
        onNodeWithContentDescription("Mark as paid: You owe Bea").assertHeightIsAtLeast(44.dp).performClick()
        assertEquals(1, asked)
    }

    @Test
    fun aDisabledPaymentAsksNothingEvenWithLargeText() = runComposeUiTest {
        var asked = 0
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                OrbitTheme {
                    OrbitTransferRow(
                        faces, "Devi a Valerio", "123,45 €", Modifier.width(320.dp),
                        action = "Paga", actionIcon = Res.drawable.ic_wallet, actionDescription = "Segna come pagato",
                        onPaid = { asked++ }, enabled = false,
                    )
                }
            }
        }
        onNodeWithText("123,45 €").assertIsDisplayed()
        onNodeWithContentDescription("Segna come pagato").assertIsNotEnabled().performClick()
        assertEquals(0, asked)
    }

    @Test
    fun withoutAnActionItOnlySaysWhatIsComing() = runComposeUiTest {
        setContent { OrbitTheme { OrbitTransferRow(faces, "Bea owes you", "12,40 €") } }
        onNodeWithText("Bea owes you").assertIsDisplayed()
        onNodeWithText("12,40 €").assertIsDisplayed()
        onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}
