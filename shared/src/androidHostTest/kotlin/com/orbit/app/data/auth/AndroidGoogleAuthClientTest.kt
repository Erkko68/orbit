package com.orbit.app.data.auth

import android.app.Activity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.test.runTest
import org.robolectric.Robolectric
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AndroidGoogleAuthClientTest : ComposeTest() {

    @Test
    fun missingActivityThrowsAuthException() = runTest {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        val client = AndroidGoogleAuthClient(context = activity, activityProvider = { null })

        val exception = assertFailsWith<AuthException> {
            client.getCredential()
        }
        assertEquals(AuthError.UNKNOWN, exception.error)
        assertEquals("Google Sign-In requires an active Activity context", exception.cause?.message)
    }

    @Test
    fun missingClientIdThrowsAuthException() = runTest {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        val client = AndroidGoogleAuthClient(context = activity, activityProvider = { activity })

        val exception = assertFailsWith<AuthException> {
            client.getCredential()
        }
        assertEquals(AuthError.UNKNOWN, exception.error)
        assertEquals("Google Sign-In is not configured: default_web_client_id string resource not found", exception.cause?.message)
    }

    @Test
    fun asksWithTheFullSignInSheetThatCanAddAnAccount() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val client = AndroidGoogleAuthClient(context = activity, activityProvider = { activity })

        val option = client.signInRequest("web-client-id").credentialOptions.single()

        assertIs<GetSignInWithGoogleOption>(option)
        assertEquals("web-client-id", option.serverClientId)
    }
}
