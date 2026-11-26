package com.aedev.aetranzit.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {

    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val digest = sha256(salt, password.toByteArray(Charsets.UTF_8))
        return "${salt.toHex()}:${digest.toHex()}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes() ?: return false
        val expectedDigest = parts[1].hexToBytes() ?: return false
        val actualDigest = sha256(salt, password.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }

    private fun sha256(salt: ByteArray, passwordBytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(passwordBytes)
        return md.digest()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { index ->
                substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }

    private companion object {
        const val SALT_BYTES = 16
    }
}
