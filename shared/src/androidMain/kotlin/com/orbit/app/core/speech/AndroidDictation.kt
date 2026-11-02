package com.orbit.app.core.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

/** [SpeechRecognizer], the system's recogniser. It has to be made and used on the main thread. */
class AndroidDictation(private val context: Context) : Dictation {
    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun listen(): Flow<String> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults.bestText()?.let { trySend(it) }
                }

                override fun onResults(results: Bundle?) {
                    results.bestText()?.let { trySend(it) }
                    close()
                }

                override fun onError(error: Int) {
                    close(DictationException(failureOf(error)))
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
        awaitClose { recognizer.destroy() }
    }.flowOn(Dispatchers.Main)

    private fun Bundle?.bestText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.ifBlank { null }
}

internal fun failureOf(error: Int): DictationFailure = when (error) {
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> DictationFailure.NO_PERMISSION
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DictationFailure.NO_SPEECH
    // Android 12 and later; older recognisers never send them, so reading the constants is safe.
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        DictationFailure.UNAVAILABLE
    else -> DictationFailure.ERROR
}
