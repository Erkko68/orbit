package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.mapper.googleSignInException
import com.orbit.app.data.mapper.mapUser
import com.orbit.app.data.mapper.registerException
import com.orbit.app.data.mapper.signInException
import com.orbit.app.data.mapper.userProfileDocument
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val googleAuthClient: GoogleAuthClient? = null,
) : AuthRepository {

    override val session: Flow<User?> = auth.authStateChanged.flatMapLatest { firebaseUser ->
        if (firebaseUser == null) {
            flowOf(null)
        } else {
            firestore.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .snapshots
                .map { snapshot ->
                    if (snapshot.exists) {
                        val displayName = try {
                            snapshot.get<String?>("displayName")
                        } catch (_: Exception) {
                            null
                        }
                        val photoUrl = try {
                            snapshot.get<String?>("photoUrl")
                        } catch (_: Exception) {
                            null
                        }
                        val language = try {
                            snapshot.get<String?>("language")
                        } catch (_: Exception) {
                            null
                        }
                        val timeZone = try {
                            snapshot.get<String?>("timeZone")
                        } catch (_: Exception) {
                            null
                        }
                        mapUser(
                            id = firebaseUser.uid,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            language = language,
                            timeZone = timeZone,
                            fallbackDisplayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                            fallbackPhotoUrl = firebaseUser.photoURL,
                        )
                    } else {
                        mapUser(
                            id = firebaseUser.uid,
                            displayName = null,
                            photoUrl = null,
                            language = null,
                            timeZone = null,
                            fallbackDisplayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                            fallbackPhotoUrl = firebaseUser.photoURL,
                        )
                    }
                }
                .catch { e ->
                    Logger.w(e) { "Failed to read user document for ${firebaseUser.uid}" }
                    emit(
                        mapUser(
                            id = firebaseUser.uid,
                            displayName = null,
                            photoUrl = null,
                            language = null,
                            timeZone = null,
                            fallbackDisplayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                            fallbackPhotoUrl = firebaseUser.photoURL,
                        )
                    )
                }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw signInException(e)
        }
    }

    override suspend fun register(email: String, password: String, displayName: String) {
        val result = try {
            auth.createUserWithEmailAndPassword(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw registerException(e)
        }
        val user = checkNotNull(result.user) { "No user returned after registration" }
        val profile = userProfileDocument(displayName = displayName) + mapOf("createdAt" to FieldValue.serverTimestamp)
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(profile, merge = true)
    }

    // Profile persistence is a separate implementation in #172.
    override suspend fun updateProfile(displayName: String, language: String, timeZone: TimeZone) {
        throw UnsupportedOperationException("Profile updates are not implemented yet (#172)")
    }

    override suspend fun signInWithGoogle(idToken: String?, accessToken: String?) {
        val credential = if (idToken != null) {
            GoogleAuthProvider.credential(idToken = idToken, accessToken = accessToken)
        } else {
            val client = checkNotNull(googleAuthClient) {
                "Google sign-in is not supported on this platform"
            }
            val googleCred = client.getCredential() ?: return
            GoogleAuthProvider.credential(idToken = googleCred.idToken, accessToken = googleCred.accessToken)
        }
        val result = try {
            auth.signInWithCredential(credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw googleSignInException(e)
        }
        val user = checkNotNull(result.user) { "No user returned after Google sign-in" }
        ensureUserProfile(user)
    }

    private suspend fun ensureUserProfile(user: FirebaseUser) {
        val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = try {
            docRef.get()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Could not check existing user profile for ${user.uid}" }
            null
        }
        if (snapshot == null || !snapshot.exists) {
            val fallbackName = user.displayName?.ifBlank { null }
                ?: user.email?.substringBefore('@').orEmpty()
            val profile = userProfileDocument(
                displayName = fallbackName,
                photoUrl = user.photoURL,
            ) + mapOf("createdAt" to FieldValue.serverTimestamp)
            try {
                docRef.set(profile, merge = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Could not create user profile for ${user.uid}" }
            }
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
