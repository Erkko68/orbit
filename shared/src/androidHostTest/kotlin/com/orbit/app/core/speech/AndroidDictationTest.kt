package com.orbit.app.core.speech

import android.speech.SpeechRecognizer
import com.orbit.app.core.designsystem.ComposeTest
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidDictationTest : ComposeTest() {
    private val app = RuntimeEnvironment.getApplication()

    @Test
    fun mapsTheRecogniserErrorsToWhatTheButtonExplains() {
        assertEquals(DictationFailure.NO_PERMISSION, failureOf(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(DictationFailure.NO_SPEECH, failureOf(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(DictationFailure.NO_SPEECH, failureOf(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(DictationFailure.UNAVAILABLE, failureOf(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertEquals(DictationFailure.UNAVAILABLE, failureOf(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(DictationFailure.ERROR, failureOf(SpeechRecognizer.ERROR_NETWORK))
    }

    @Test
    fun needsTheMicrophonePermission() = runBlocking {
        val dictation = AndroidDictation(app)
        assertFalse(dictation.hasPermission())

        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        assertTrue(dictation.hasPermission())
    }
}
