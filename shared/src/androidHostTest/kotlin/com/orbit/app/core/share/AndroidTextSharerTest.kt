package com.orbit.app.core.share

import android.content.Intent
import com.orbit.app.core.designsystem.ComposeTest
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AndroidTextSharerTest : ComposeTest() {
    @Test
    @Suppress("DEPRECATION")
    fun opensTheChooserWithTheTextToSend() {
        val application = RuntimeEnvironment.getApplication()

        AndroidTextSharer(application).share("Join Flat 4B on Orbit")

        val chooser = assertNotNull(shadowOf(application).nextStartedActivity)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        val send = assertNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals("Join Flat 4B on Orbit", send.getStringExtra(Intent.EXTRA_TEXT))
    }
}
