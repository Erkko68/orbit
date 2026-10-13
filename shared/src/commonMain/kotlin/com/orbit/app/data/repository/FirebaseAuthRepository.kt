package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.mapUser
import com.orbit.app.data.mapper.registerException
import com.orbit.app.data.mapper.signInException
import com.orbit.app.data.mapper.userProfileDocument
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
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

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
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
                        val displayName = try { snapshot.get<String?>("displayName") } catch (_: Exception) { null }
                        val photoUrl = try { snapshot.get<String?>("photoUrl") } catch (_: Exception) { null }
                        val language = try { snapshot.get<String?>("language") } catch (_: Exception) { null }
                        val timeZone = try { snapshot.get<String?>("timeZone") } catch (_: Exception) { null }
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

    override suspend fun signOut() {
        auth.signOut()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
