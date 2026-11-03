package com.orbit.app.core.speech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.AVFAudio.AVAudioSession
import platform.Speech.SFSpeechRecognizer
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Speech recognition first, then the microphone: each prompt shows once per install, after which iOS answers
 * with the stored choice at once.
 */
@Composable
actual fun rememberDictationPermissionPrompt(onAnswer: (allowed: Boolean) -> Unit): () -> Unit {
    val currentOnAnswer by rememberUpdatedState(onAnswer)
    return remember {
        {
            SFSpeechRecognizer.requestAuthorization { status ->
                if (status != speechAuthorized) {
                    dispatch_async(dispatch_get_main_queue()) { currentOnAnswer(false) }
                } else {
                    AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                        // The answers come on a background queue; the screen takes them on the main one.
                        dispatch_async(dispatch_get_main_queue()) { currentOnAnswer(granted) }
                    }
                }
            }
        }
    }
}
