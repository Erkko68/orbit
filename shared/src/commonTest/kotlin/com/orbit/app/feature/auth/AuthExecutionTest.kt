package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthExecutionTest {

    @Test
    fun returnsNullWhenBlockSucceeds() = runTest {
        val result = executeAuth("Success") {
            // success
        }
        assertNull(result)
    }

    @Test
    fun returnsAuthErrorWhenAuthExceptionIsThrown() = runTest {
        val result = executeAuth("Failure") {
            throw AuthException(AuthError.WRONG_CREDENTIALS)
        }
        assertEquals(AuthError.WRONG_CREDENTIALS, result)
    }

    @Test
    fun returnsUnknownWhenGenericExceptionIsThrown() = runTest {
        val result = executeAuth("Crash") {
            throw IllegalStateException("Generic failure")
        }
        assertEquals(AuthError.UNKNOWN, result)
    }

    @Test
    fun rethrowsCancellationException() = runTest {
        assertFailsWith<CancellationException> {
            executeAuth("Cancel") {
                throw CancellationException("Cancelled")
            }
        }
    }
}
