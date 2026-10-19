package com.orbit.app.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import co.touchlab.kermit.Logger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import kotlinx.coroutines.CancellationException

class AndroidGoogleAuthClient(
    private val context: Context,
    private val activityProvider: () -> Activity? = { context as? Activity },
) : GoogleAuthClient {

    override suspend fun getCredential(): GoogleAuthCredential? {
        val activity = activityProvider()
            ?: throw AuthException(
                AuthError.UNKNOWN,
                IllegalStateException("Google Sign-In requires an active Activity context"),
            )

        val serverClientId = resolveServerClientId(activity)
            ?: throw AuthException(
                AuthError.UNKNOWN,
                IllegalStateException("Google Sign-In is not configured: default_web_client_id string resource not found"),
            )

        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = activity, request = request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleAuthCredential(
                    idToken = googleIdTokenCredential.idToken,
                    accessToken = null,
                )
            } else {
                Logger.w { "Unexpected credential type: ${credential::class.simpleName}" }
                throw AuthException(AuthError.UNKNOWN)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Logger.d { "Google sign-in cancelled by user" }
            null
        } catch (e: GetCredentialException) {
            Logger.e(e) { "Credential Manager error" }
            throw AuthException(AuthError.UNKNOWN, e)
        } catch (e: Exception) {
            Logger.e(e) { "Google sign-in unexpected error" }
            throw AuthException(AuthError.UNKNOWN, e)
        }
    }

    private fun resolveServerClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId).takeIf { it.isNotBlank() }
        } else {
            null
        }
    }
}
