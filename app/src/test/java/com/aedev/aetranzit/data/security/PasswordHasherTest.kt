package com.aedev.aetranzit.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test
    fun `hash produces salted digest and verifies matching password`() {
        val hash1 = hasher.hash("secret123")
        val hash2 = hasher.hash("secret123")

        assertNotEquals(hash1, hash2)
        assertTrue(hasher.verify("secret123", hash1))
        assertTrue(hasher.verify("secret123", hash2))
    }

    @Test
    fun `verify rejects wrong password or malformed stored hash`() {
        val stored = hasher.hash("secret123")

        assertFalse(hasher.verify("wrong123", stored))
        assertFalse(hasher.verify("secret123", "malformed"))
        assertFalse(hasher.verify("secret123", "zz:1234"))
    }
}
