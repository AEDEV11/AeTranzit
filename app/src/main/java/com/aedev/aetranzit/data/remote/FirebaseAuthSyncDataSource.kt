package com.aedev.aetranzit.data.remote

import com.aedev.aetranzit.data.local.entity.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSyncDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun pushUserProfile(user: UserEntity, rawPassword: String? = null): Result<Unit> {
        return try {
            if (rawPassword != null && firebaseAuth.currentUser?.email.equals(user.email, ignoreCase = true).not()) {
                runCatching {
                    firebaseAuth.createUserWithEmailAndPassword(user.email, rawPassword).await()
                }.recoverCatching {
                    firebaseAuth.signInWithEmailAndPassword(user.email, rawPassword).await()
                }.getOrThrow()
            }
            val payload = mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "role" to user.role,
                "isActive" to user.isActive,
                "updatedAt" to user.updatedAt
            )
            val currentAuthUser = firebaseAuth.currentUser
            val docId = if (
                currentAuthUser != null &&
                currentAuthUser.email.equals(user.email, ignoreCase = true)
            ) {
                currentAuthUser.uid
            } else {
                user.id
            }
            firestore.collection(USERS_COLLECTION)
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAndFetchProfile(email: String, password: String): Result<UserProfileRemote?> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.success(null)
            val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (!snapshot.exists()) {
                return Result.success(null)
            }
            val profile = UserProfileRemote(
                id = snapshot.getString("id") ?: uid,
                name = snapshot.getString("name") ?: authResult.user?.displayName.orEmpty(),
                email = snapshot.getString("email") ?: email,
                role = snapshot.getString("role") ?: "CONDUCTOR",
                isActive = snapshot.getBoolean("isActive") ?: true,
                updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
            )
            Result.success(profile)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        runCatching { firebaseAuth.signOut() }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}

data class UserProfileRemote(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val updatedAt: Long
)
