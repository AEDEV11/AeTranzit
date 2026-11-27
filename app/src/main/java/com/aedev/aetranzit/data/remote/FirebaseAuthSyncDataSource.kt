package com.aedev.aetranzit.data.remote

import android.content.Context
import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.domain.model.Company
import com.aedev.aetranzit.domain.model.UserRole
import com.aedev.aetranzit.domain.sync.TransientSyncException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthSyncRemoteDataSource {
    suspend fun pushCompanySetup(
        company: CompanyEntity,
        user: UserEntity
    ): Result<Unit>

    suspend fun provisionStaffAccount(
        user: UserEntity,
        temporaryPassword: String
    ): Result<Unit>

    suspend fun updateCurrentUserPasswordAndProfile(
        user: UserEntity,
        newPassword: String
    ): Result<Unit>

    suspend fun ensureFirebaseAuthSession(
        user: UserEntity,
        company: CompanyEntity?,
        rawPassword: String
    ): Result<Unit>

    suspend fun pushUserProfile(user: UserEntity, rawPassword: String? = null): Result<Unit>

    suspend fun signInAndFetchProfile(email: String, password: String): Result<UserProfileRemote?>

    fun signOut()
}

@Singleton
class FirebaseAuthSyncDataSource @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthSyncRemoteDataSource {

    private val staffProvisionMutex = Mutex()

    override suspend fun pushCompanySetup(
        company: CompanyEntity,
        user: UserEntity
    ): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
                ?: return Result.failure(
                    TransientSyncException(
                        "Cloud authentication session unavailable. Business setup saved locally and will sync when online."
                    )
                )

            commitFounderCompanyAndProfileBatch(uid = uid, company = company, user = user)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "Admin business setup"))
        }
    }

    override suspend fun provisionStaffAccount(
        user: UserEntity,
        temporaryPassword: String
    ): Result<Unit> = staffProvisionMutex.withLock {
        val currentAdminAuth = firebaseAuth.currentUser
            ?: return@withLock Result.failure(
                TransientSyncException(
                    "Admin cloud session is not active yet. Staff account saved locally and will sync when online."
                )
            )
        if (currentAdminAuth.uid.isBlank()) {
            return@withLock Result.failure(
                TransientSyncException("Admin cloud session unavailable.")
            )
        }

        var secondaryApp: FirebaseApp? = null
        var secondaryAuth: FirebaseAuth? = null
        try {
            val primaryApp = FirebaseApp.getInstance()
            FirebaseApp.getApps(appContext)
                .firstOrNull { it.name == STAFF_PROVISIONER_APP_NAME }
                ?.let { staleApp ->
                    runCatching { FirebaseAuth.getInstance(staleApp).signOut() }
                    runCatching { staleApp.delete() }
                }

            val createdApp = FirebaseApp.initializeApp(
                appContext,
                primaryApp.options,
                STAFF_PROVISIONER_APP_NAME
            )
            secondaryApp = createdApp
            val tempAuth = FirebaseAuth.getInstance(createdApp)
            secondaryAuth = tempAuth

            val authResult = tempAuth
                .createUserWithEmailAndPassword(user.email, temporaryPassword)
                .await()
            val newUid = authResult.user?.uid
                ?: return@withLock Result.failure(
                    IllegalStateException("Failed to obtain UID for newly provisioned staff account.")
                )

            val payload = buildUserPayload(user.copy(mustChangePassword = true))
            firestore.collection(USERS_COLLECTION)
                .document(newUid)
                .set(payload, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "staff account provisioning"))
        } finally {
            runCatching { secondaryAuth?.signOut() }
            runCatching { secondaryApp?.delete() }
        }
    }

    override suspend fun updateCurrentUserPasswordAndProfile(
        user: UserEntity,
        newPassword: String
    ): Result<Unit> {
        return try {
            val currentAuthUser = firebaseAuth.currentUser
                ?: return Result.failure(
                    TransientSyncException(
                        "Firebase Auth session is offline. Password updated locally and profile will sync when online."
                    )
                )

            if (!currentAuthUser.email.equals(user.email, ignoreCase = true)) {
                return Result.failure(
                    TransientSyncException(
                        "Active cloud session does not match ${user.email}. Will sync on next online login."
                    )
                )
            }

            currentAuthUser.updatePassword(newPassword).await()
            val payload = buildUserPayload(user.copy(mustChangePassword = false))
            firestore.collection(USERS_COLLECTION)
                .document(currentAuthUser.uid)
                .set(payload, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "forced password update"))
        }
    }

    override suspend fun ensureFirebaseAuthSession(
        user: UserEntity,
        company: CompanyEntity?,
        rawPassword: String
    ): Result<Unit> {
        return try {
            val currentAuth = firebaseAuth.currentUser
            if (currentAuth == null || !currentAuth.email.equals(user.email, ignoreCase = true)) {
                if (currentAuth != null) {
                    runCatching { firebaseAuth.signOut() }
                }
                try {
                    firebaseAuth.signInWithEmailAndPassword(user.email, rawPassword).await()
                } catch (_: FirebaseAuthInvalidUserException) {
                    firebaseAuth.createUserWithEmailAndPassword(user.email, rawPassword).await()
                }
            }

            val uid = firebaseAuth.currentUser?.uid
                ?: return Result.failure(
                    IllegalStateException("Unable to establish Firebase Auth session for ${user.email}.")
                )

            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
            val existingProfile = userRef.get().await()
            val isAdmin = UserRole.fromRaw(user.role) == UserRole.ADMIN
            val hasConfiguredCompany = company != null && user.companyId.isNotBlank()

            if (!existingProfile.exists()) {
                if (isAdmin && hasConfiguredCompany) {
                    val companyRef = firestore.collection(COMPANIES_COLLECTION).document(company.id)
                    val existingCompany = companyRef.get().await()
                    if (!existingCompany.exists()) {
                        commitFounderCompanyAndProfileBatch(uid = uid, company = company, user = user)
                    } else {
                        userRef.set(buildUserPayload(user), SetOptions.merge()).await()
                    }
                } else {
                    userRef.set(buildUserPayload(user), SetOptions.merge()).await()
                }
            } else if (
                isAdmin &&
                hasConfiguredCompany &&
                existingProfile.getString("companyId").orEmpty().isBlank()
            ) {
                commitFounderCompanyAndProfileBatch(uid = uid, company = company, user = user)
            }

            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "Firebase Auth session link"))
        }
    }

    override suspend fun pushUserProfile(user: UserEntity, rawPassword: String?): Result<Unit> {
        return try {
            if (rawPassword != null) {
                ensureSignedIn(user.email, rawPassword)
            }
            val currentAuthUser = firebaseAuth.currentUser
                ?: return Result.failure(
                    IllegalStateException(
                        "Firebase Auth session is not signed in. Log in once while online to link your cloud session."
                    )
                )

            val payload = buildUserPayload(user)
            val docId = if (currentAuthUser.email.equals(user.email, ignoreCase = true)) {
                currentAuthUser.uid
            } else {
                val existingQuery = firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("companyId", user.companyId)
                    .whereEqualTo("id", user.id)
                    .limit(1)
                    .get()
                    .await()
                existingQuery.documents.firstOrNull()?.id ?: user.id
            }

            firestore.collection(USERS_COLLECTION)
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "user profile sync"))
        }
    }

    override suspend fun signInAndFetchProfile(
        email: String,
        password: String
    ): Result<UserProfileRemote?> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.success(null)
            val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (!snapshot.exists()) {
                return Result.success(null)
            }

            val roleRaw = snapshot.getString("role") ?: "CONDUCTOR"
            val parsedRole = UserRole.fromRaw(roleRaw)
            val resolvedCompanyId = if (snapshot.contains("companyId")) {
                snapshot.getString("companyId")?.trim().orEmpty()
            } else if (parsedRole == UserRole.ADMIN) {
                ""
            } else {
                Company.DEFAULT_COMPANY_ID
            }

            val companyEntity = if (resolvedCompanyId.isNotBlank()) {
                runCatching {
                    val compSnap = firestore.collection(COMPANIES_COLLECTION)
                        .document(resolvedCompanyId)
                        .get()
                        .await()
                    if (compSnap.exists()) {
                        val now = System.currentTimeMillis()
                        CompanyEntity(
                            id = compSnap.getString("id") ?: compSnap.id,
                            name = compSnap.getString("name").orEmpty().ifBlank { Company.DEFAULT_COMPANY_NAME },
                            joinCode = compSnap.getString("joinCode").orEmpty().ifBlank { Company.DEFAULT_JOIN_CODE },
                            createdAt = compSnap.getLong("createdAt") ?: now,
                            isSynced = true,
                            updatedAt = compSnap.getLong("updatedAt") ?: now
                        )
                    } else {
                        null
                    }
                }.getOrNull()
            } else {
                null
            }

            val profile = UserProfileRemote(
                id = snapshot.getString("id") ?: uid,
                name = snapshot.getString("name") ?: authResult.user?.displayName.orEmpty(),
                email = snapshot.getString("email") ?: email,
                role = parsedRole.name,
                companyId = resolvedCompanyId,
                isActive = snapshot.getBoolean("isActive") ?: true,
                mustChangePassword = snapshot.getBoolean("mustChangePassword") ?: false,
                updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis(),
                company = companyEntity
            )
            Result.success(profile)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(FirebaseSyncErrorMapper.map(e, "cloud sign-in"))
        }
    }

    override fun signOut() {
        runCatching { firebaseAuth.signOut() }
    }

    private suspend fun ensureSignedIn(email: String, rawPassword: String) {
        if (!firebaseAuth.currentUser?.email.equals(email, ignoreCase = true)) {
            if (firebaseAuth.currentUser != null) {
                runCatching { firebaseAuth.signOut() }
            }
            runCatching {
                firebaseAuth.signInWithEmailAndPassword(email, rawPassword).await()
            }.recoverCatching {
                firebaseAuth.createUserWithEmailAndPassword(email, rawPassword).await()
            }.getOrThrow()
        }
    }

    private suspend fun commitFounderCompanyAndProfileBatch(
        uid: String,
        company: CompanyEntity,
        user: UserEntity
    ) {
        val companyPayload = mapOf(
            "id" to company.id,
            "name" to company.name,
            "joinCode" to company.joinCode,
            "ownerUid" to uid,
            "createdAt" to company.createdAt,
            "updatedAt" to company.updatedAt
        )
        val userPayload = buildUserPayload(user)
        val batch = firestore.batch()
        val companyRef = firestore.collection(COMPANIES_COLLECTION).document(company.id)
        val userRef = firestore.collection(USERS_COLLECTION).document(uid)
        batch.set(companyRef, companyPayload, SetOptions.merge())
        batch.set(userRef, userPayload, SetOptions.merge())
        batch.commit().await()
    }

    private fun buildUserPayload(user: UserEntity): Map<String, Any> = mapOf(
        "id" to user.id,
        "name" to user.name,
        "email" to user.email,
        "role" to user.role,
        "companyId" to user.companyId,
        "isActive" to user.isActive,
        "mustChangePassword" to user.mustChangePassword,
        "updatedAt" to user.updatedAt
    )

    private companion object {
        const val STAFF_PROVISIONER_APP_NAME = "staff_provisioner"
        const val COMPANIES_COLLECTION = "companies"
        const val USERS_COLLECTION = "users"
    }
}

data class UserProfileRemote(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val companyId: String,
    val isActive: Boolean,
    val mustChangePassword: Boolean = false,
    val updatedAt: Long,
    val company: CompanyEntity? = null
)
