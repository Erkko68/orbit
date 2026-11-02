package com.orbit.app.core.speech

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

/**
 * [SFSpeechRecognizer] in the phone's language, fed from the microphone through an [AVAudioEngine]. Both the
 * microphone and speech recognition have to be allowed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDictation : Dictation {
    private fun recognizer() = SFSpeechRecognizer(locale = NSLocale.currentLocale)

    override fun isAvailable(): Boolean = recognizer().available

    override suspend fun hasPermission(): Boolean =
        SFSpeechRecognizer.authorizationStatus() == speechAuthorized &&
            AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted

    override fun listen(): Flow<String> = callbackFlow {
        val recognizer = recognizer()
        if (!recognizer.available) throw DictationException(DictationFailure.UNAVAILABLE)

        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, AVAudioSessionModeMeasurement, AVAudioSessionCategoryOptionDuckOthers, null)
        session.setActive(true, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)

        val request = SFSpeechAudioBufferRecognitionRequest().apply { shouldReportPartialResults = true }
        val engine = AVAudioEngine()
        val input = engine.inputNode
        input.installTapOnBus(0u, 1024u, input.outputFormatForBus(0u)) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }
        engine.prepare()
        if (!engine.startAndReturnError(null)) throw DictationException(DictationFailure.ERROR)

        val task = recognizer.recognitionTaskWithRequest(request) { result, error ->
            when {
                result != null -> {
                    result.bestTranscription.formattedString.ifBlank { null }?.let { trySend(it) }
                    if (result.final) close()
                }
                error != null -> close(DictationException(DictationFailure.NO_SPEECH))
            }
        }

        awaitClose {
            engine.stop()
            input.removeTapOnBus(0u)
            request.endAudio()
            task.cancel()
            session.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
        }
    }
}

internal val speechAuthorized = SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
