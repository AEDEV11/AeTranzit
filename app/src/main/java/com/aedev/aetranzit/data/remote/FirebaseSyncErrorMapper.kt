package com.aedev.aetranzit.data.remote

import com.aedev.aetranzit.domain.sync.TransientSyncException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object FirebaseSyncErrorMapper {

    fun map(error: Throwable, operation: String): Exception {
        if (error is TransientSyncException) return error

        return when (error) {
            is FirebaseNetworkException,
            is UnknownHostException,
            is SocketTimeoutException -> TransientSyncException(
                "Network connection unavailable during $operation. Changes are queued locally and will auto-sync when online.",
                error
            )

            is IOException -> TransientSyncException(
                "Network I/O interrupted during $operation. Will retry automatically when online.",
                error
            )

            is FirebaseFirestoreException -> mapFirestoreException(error, operation)

            is FirebaseAuthInvalidCredentialsException -> IllegalArgumentException(
                "Invalid email or password for cloud account ($operation).",
                error
            )

            is FirebaseAuthInvalidUserException -> IllegalArgumentException(
                "Cloud account does not exist yet ($operation).",
                error
            )

            is FirebaseAuthUserCollisionException -> IllegalStateException(
                "An account with this email address already exists in cloud ($operation).",
                error
            )

            is FirebaseAuthException -> IllegalStateException(
                "Firebase Auth error (${error.errorCode}) during $operation: ${error.localizedMessage ?: "Authentication failed."}",
                error
            )

            is Exception -> error
            else -> IllegalStateException(
                error.message ?: "Unexpected cloud sync failure during $operation.",
                error
            )
        }
    }

    private fun mapFirestoreException(
        error: FirebaseFirestoreException,
        operation: String
    ): Exception {
        return when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> TransientSyncException(
                "Cloud Firestore temporarily unavailable during $operation (${error.code.name}). Will retry automatically.",
                error
            )

            FirebaseFirestoreException.Code.PERMISSION_DENIED -> IllegalStateException(
                "Cloud access denied during $operation. Verify your account is signed in and active in your bus company.",
                error
            )

            FirebaseFirestoreException.Code.UNAUTHENTICATED -> IllegalStateException(
                "Cloud authentication session expired during $operation. Sign out and sign back in while online.",
                error
            )

            else -> IllegalStateException(
                "Cloud sync error (${error.code.name}) during $operation: ${error.localizedMessage ?: "Operation failed."}",
                error
            )
        }
    }
}
