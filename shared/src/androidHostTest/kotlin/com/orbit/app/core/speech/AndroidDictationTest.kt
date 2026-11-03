package com.orbit.app.core.speech

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.orbit.app.core.designsystem.ComposeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSpeechRecognizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDictationTest : ComposeTest() {
    private val app = RuntimeEnvironment.getApplication()

    private fun heard(text: String) =
        Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text)) }

    /** Runs [block] with the main dispatcher on the test's, so the recogniser's callbacks run in the test. */
    private fun listening(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emitsWhatItHearsUntilTheUserStopsTalking() = listening {
        val text = async { AndroidDictation(app).listen().toList() }
        runCurrent()
        shadowOf(Looper.getMainLooper()).idle()
        val recognizer = shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer())

        recognizer.triggerOnPartialResults(heard("Three"))
        recognizer.triggerOnResults(heard("Three of us"))

        assertEquals(listOf("Three", "Three of us"), text.await())
    }

    @Test
    fun aRecogniserErrorEndsItWithTheReason() = listening {
        val text = async { runCatching { AndroidDictation(app).listen().toList() } }
        runCurrent()
        shadowOf(Looper.getMainLooper()).idle()

        shadowOf(ShadowSpeechRecognizer.getLatestSpeechRecognizer()).triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)

        val failure = assertFailsWith<DictationException> { text.await().getOrThrow() }
        assertEquals(DictationFailure.NO_SPEECH, failure.failure)
    }

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

        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        assertTrue(dictation.hasPermission())
    }

    @Test
    fun aPhoneWithoutARecogniserCannotDictate() {
        assertFalse(AndroidDictation(app).isAvailable())
    }

    @Test
    @Suppress("DEPRECATION")
    fun thePromptAsksForTheMicrophone() {
        val answers = mutableListOf<Boolean>()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        lateinit var prompt: () -> Unit
        activity.setContent { prompt = rememberDictationPermissionPrompt { answers += it } }
        shadowOf(Looper.getMainLooper()).idle()

        prompt()

        val request = assertNotNull(shadowOf(activity).lastRequestedPermission)
        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), request.requestedPermissions.toList())
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            intArrayOf(PackageManager.PERMISSION_GRANTED),
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(true), answers)
    }
}
