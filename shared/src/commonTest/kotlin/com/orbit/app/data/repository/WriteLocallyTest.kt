package com.orbit.app.data.repository

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WriteLocallyTest {
    @Test
    fun returnsOnceTheServerConfirms() = runTest {
        var written = false

        writeLocally("Item 1") { written = true }

        assertEquals(true, written)
        assertEquals(0, currentTime)
    }

    @Test
    fun returnsAfterAWhileWhenTheServerCannotBeReached() = runTest {
        writeLocally("Item 1") { awaitCancellation() }

        assertEquals(3_000, currentTime)
    }

    @Test
    fun aRefusedWriteThrows() = runTest {
        assertFailsWith<IllegalStateException> {
            writeLocally("Item 1") { error("permission denied") }
        }
    }

    @Test
    fun writingOnTheServerReturnsOnceItConfirms() = runTest {
        var written = false

        writeOnServer("Invitation abc") { written = true }

        assertEquals(true, written)
        assertEquals(0, currentTime)
    }

    @Test
    fun writingOnTheServerFailsWhenItCannotBeReached() = runTest {
        assertFailsWith<IllegalStateException> {
            writeOnServer("Invitation abc") { awaitCancellation() }
        }

        assertEquals(10_000, currentTime)
    }

    @Test
    fun writingOnTheServerThrowsWhenItRefuses() = runTest {
        assertFailsWith<IllegalStateException> {
            writeOnServer("Invitation abc") { error("permission denied") }
        }
    }
}
