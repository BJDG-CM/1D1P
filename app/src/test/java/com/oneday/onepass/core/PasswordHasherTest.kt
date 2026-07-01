package com.oneday.onepass.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    private val hasher = PasswordHasher()

    @Test
    fun `verify succeeds for the correct password`() {
        val hashed = hasher.hash("12345678")
        assertTrue(hasher.verify("12345678", hashed.saltHex, hashed.hashHex))
    }

    @Test
    fun `verify fails for an incorrect password`() {
        val hashed = hasher.hash("12345678")
        assertFalse(hasher.verify("87654321", hashed.saltHex, hashed.hashHex))
        assertFalse(hasher.verify("1234567", hashed.saltHex, hashed.hashHex))
        assertFalse(hasher.verify("123456789", hashed.saltHex, hashed.hashHex))
    }

    @Test
    fun `hashing is salted - same password yields different salt and hash each time`() {
        val a = hasher.hash("11112222")
        val b = hasher.hash("11112222")
        assertNotEquals("salts must differ", a.saltHex, b.saltHex)
        assertNotEquals("hashes must differ due to salt", a.hashHex, b.hashHex)
        // but both still verify against the same password
        assertTrue(hasher.verify("11112222", a.saltHex, a.hashHex))
        assertTrue(hasher.verify("11112222", b.saltHex, b.hashHex))
    }

    @Test
    fun `plaintext password never appears in stored representation`() {
        val pw = "90908080"
        val hashed = hasher.hash(pw)
        assertFalse(hashed.hashHex.contains(pw))
        assertFalse(hashed.saltHex.contains(pw))
    }

    @Test
    fun `hash output is a 64-char hex SHA-256 digest with a 32-char hex salt`() {
        val hashed = hasher.hash("55554444")
        assertEquals(64, hashed.hashHex.length) // 32 bytes
        assertEquals(PasswordHasher.SALT_BYTES * 2, hashed.saltHex.length)
        assertTrue(hashed.hashHex.all { it in "0123456789abcdef" })
    }

    @Test
    fun `verify fails when stored hash is tampered`() {
        val hashed = hasher.hash("12121212")
        val tampered = hashed.hashHex.replaceRange(0, 1, if (hashed.hashHex[0] == 'a') "b" else "a")
        assertFalse(hasher.verify("12121212", hashed.saltHex, tampered))
    }

    @Test
    fun `isValidPassword enforces exactly 8 digits`() {
        assertTrue(PasswordHasher.isValidPassword("00000000"))
        assertTrue(PasswordHasher.isValidPassword("48213907"))
        assertFalse(PasswordHasher.isValidPassword("1234567"))   // too short
        assertFalse(PasswordHasher.isValidPassword("123456789")) // too long
        assertFalse(PasswordHasher.isValidPassword("1234567a"))  // non-digit
        assertFalse(PasswordHasher.isValidPassword(""))
    }
}
