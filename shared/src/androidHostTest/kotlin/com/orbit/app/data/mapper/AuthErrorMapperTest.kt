package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.core.designsystem.ComposeTest
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** Android only: the Firebase exceptions can only be built here, as the SDK's own classes, under Robolectric. */
class AuthErrorMapperTest : ComposeTest() {
    private val invalidCredentials = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad")
    private val invalidUser = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no user")
    private val weakPassword = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")
    private val collision = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken")
    private val network = FirebaseNetworkException("offline")

    @Test
    fun signingInWithTheWrongDetailsIsWrongCredentialsWhetherOrNotTheAccountExists() {
        assertEquals(AuthError.WRONG_CREDENTIALS, signInException(invalidCredentials).error)
        assertEquals(AuthError.WRONG_CREDENTIALS, signInException(invalidUser).error)
    }

    @Test
    fun registeringWithAnEmailFirebaseRejectsIsAnInvalidEmail() {
        assertEquals(AuthError.INVALID_EMAIL, registerException(invalidCredentials).error)
        assertEquals(AuthError.INVALID_EMAIL, registerException(invalidUser).error)
    }

    @Test
    fun aWeakPasswordIsNotMistakenForInvalidCredentials() {
        assertEquals(AuthError.WEAK_PASSWORD, registerException(weakPassword).error)
        assertEquals(AuthError.WEAK_PASSWORD, signInException(weakPassword).error)
    }

    @Test
    fun anExistingEmailIsInUse() {
        assertEquals(AuthError.EMAIL_IN_USE, registerException(collision).error)
    }

    @Test
    fun noConnectionIsANetworkError() {
        assertEquals(AuthError.NETWORK, signInException(network).error)
        assertEquals(AuthError.NETWORK, registerException(network).error)
    }

    @Test
    fun anythingElseIsUnknownAndKeepsItsCause() {
        val cause = IllegalStateException("boom")

        val exception: AuthException = signInException(cause)

        assertEquals(AuthError.UNKNOWN, exception.error)
        assertSame(cause, exception.cause)
    }

    @Test
    fun googleSignInErrorsMapAppropriately() {
        assertEquals(AuthError.NETWORK, (googleSignInException(network) as AuthException).error)
        assertEquals(AuthError.EMAIL_IN_USE, (googleSignInException(collision) as AuthException).error)
        assertEquals(AuthError.UNKNOWN, (googleSignInException(invalidCredentials) as AuthException).error)

        val existing = AuthException(AuthError.UNKNOWN)
        assertSame(existing, googleSignInException(existing))
    }
}
