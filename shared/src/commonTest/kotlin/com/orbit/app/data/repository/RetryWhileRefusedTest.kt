package com.orbit.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RetryWhileRefusedTest {
    private class Refused : Exception()

    private fun refused(failure: Throwable) = failure is Refused

    @Test
    fun listensAgainAfterEachRefusalUntilItIsAccepted() = runTest {
        var listens = 0
        val members = flow {
            if (++listens <= 2) throw Refused()
            emit("ana")
        }

        val first = members.retryWhileRefused(retries = 5, wait = 1.seconds, isRefused = ::refused).first()

        assertEquals("ana", first)
        assertEquals(3, listens)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun givesTheRefusalToTheCallerOnceItHasTriedEnough() = runTest {
        var listens = 0
        val members = flow<String> {
            listens++
            throw Refused()
        }

        assertFailsWith<Refused> {
            members.retryWhileRefused(retries = 3, wait = 1.seconds, isRefused = ::refused).first()
        }

        assertEquals(4, listens)
    }

    @Test
    fun doesNotRetryAnyOtherFailure() = runTest {
        var listens = 0
        val members = flow<String> {
            listens++
            error("boom")
        }

        assertFailsWith<IllegalStateException> {
            members.retryWhileRefused(retries = 3, wait = 1.seconds, isRefused = ::refused).first()
        }

        assertEquals(1, listens)
        assertEquals(0, currentTime)
    }
}
