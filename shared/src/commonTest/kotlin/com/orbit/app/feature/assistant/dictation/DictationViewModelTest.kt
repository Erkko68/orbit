package com.orbit.app.feature.assistant.dictation

import com.orbit.app.core.speech.DictationException
import com.orbit.app.core.speech.DictationFailure
import com.orbit.app.testing.FakeDictation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DictationViewModelTest {
    private val dictation = FakeDictation()
    private val viewModel by lazy { DictationViewModel(dictation) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listensAndKeepsWhatItHears() {
        viewModel.toggle()
        dictation.say("Three")
        dictation.say("Three of us")

        val state = viewModel.uiState.value
        assertTrue(state.listening)
        assertEquals(1, state.started)
        assertEquals("Three of us", state.transcript)

        dictation.finish()
        assertFalse(viewModel.uiState.value.listening)
        assertEquals("Three of us", viewModel.uiState.value.transcript)
    }

    @Test
    fun theButtonStopsItWhileListening() {
        viewModel.toggle()
        dictation.say("Three")

        viewModel.toggle()

        assertFalse(viewModel.uiState.value.listening)
        assertEquals("Three", viewModel.uiState.value.transcript)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun eachDictationStartsAfresh() {
        viewModel.toggle()
        dictation.say("Three of us")
        dictation.finish()

        viewModel.toggle()

        assertEquals(2, viewModel.uiState.value.started)
        assertEquals("", viewModel.uiState.value.transcript)
    }

    @Test
    fun aPhoneWithoutARecogniserSaysSoWithoutListening() {
        dictation.available = false

        viewModel.toggle()

        assertEquals(DictationFailure.UNAVAILABLE, viewModel.uiState.value.failure)
        assertEquals(0, dictation.listens)

        viewModel.dismissFailure()
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun asksForTheMicrophoneOnceAndListensWhenAllowed() {
        dictation.allowed = false

        viewModel.toggle()
        assertTrue(viewModel.uiState.value.askingPermission)

        viewModel.onPermissionPromptShown()
        assertFalse(viewModel.uiState.value.askingPermission)

        viewModel.onPermissionAnswer(true)
        assertTrue(viewModel.uiState.value.listening)
    }

    @Test
    fun aRefusedMicrophoneSaysWhy() {
        dictation.allowed = false
        viewModel.toggle()
        viewModel.onPermissionPromptShown()

        viewModel.onPermissionAnswer(false)

        assertEquals(DictationFailure.NO_PERMISSION, viewModel.uiState.value.failure)
        assertEquals(0, dictation.listens)
    }

    @Test
    fun aRecogniserFailureSaysWhy() {
        viewModel.toggle()

        dictation.fail(DictationException(DictationFailure.NO_SPEECH))

        assertEquals(DictationFailure.NO_SPEECH, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.listening)
    }

    @Test
    fun anUnexpectedFailureIsAnError() {
        viewModel.toggle()

        dictation.fail(IllegalStateException("audio"))

        assertEquals(DictationFailure.ERROR, viewModel.uiState.value.failure)
    }
}
