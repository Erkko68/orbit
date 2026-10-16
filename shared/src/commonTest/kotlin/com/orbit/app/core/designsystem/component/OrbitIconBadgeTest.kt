package com.orbit.app.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_list
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitIconBadgeTest : ComposeTest() {
    @Test
    fun isA40dpCircle() = runComposeUiTest {
        setContent { OrbitTheme { OrbitIconBadge(Res.drawable.ic_list, Modifier.testTag("badge")) } }
        onNodeWithTag("badge").assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }
}
