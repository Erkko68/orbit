package com.orbit.app.data.auth

import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IosGoogleAuthClientTest {

    private fun client(idToken: String?, accessToken: String?, error: String?) =
        IosGoogleAuthClient { onResult -> onResult(idToken, accessToken, error) }

    @Test
    fun tokenBecomesCredential() = runTest {
        assertEquals(GoogleAuthCredential("id", "access"), client("id", "access", null).getCredential())
    }

    @Test
    fun closedPickerReturnsNull() = runTest {
        assertNull(client(null, null, null).getCredential())
    }

    @Test
    fun errorThrowsAuthException() = runTest {
        assertFailsWith<AuthException> { client(null, null, "boom").getCredential() }
    }
}
