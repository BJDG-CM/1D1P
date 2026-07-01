package com.oneday.onepass.core

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hashes and verifies the 8-digit app password.
 *
 * The plaintext password is never stored. We store `salt` + `SHA-256(salt || password)`.
 * A per-password random salt means two identical passwords hash to different values, and verifying
 * requires re-hashing the candidate with the stored salt.
 *
 * Pure Kotlin/JVM (no Android APIs) so it is directly unit testable.
 */
class PasswordHasher(private val random: SecureRandom = SecureRandom()) {

    /** Immutable result of hashing: both parts are hex strings, safe to persist as text. */
    data class Hashed(val saltHex: String, val hashHex: String)

    /** Creates a fresh random salt and hashes [password] with it. */
    fun hash(password: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = digest(salt, password)
        return Hashed(salt.toHex(), hash.toHex())
    }

    /**
     * Re-hashes [candidate] with the stored [saltHex] and compares (constant-time) against
     * [expectedHashHex].
     *
     * @return true only if the candidate matches the stored hash.
     */
    fun verify(candidate: String, saltHex: String, expectedHashHex: String): Boolean {
        val salt = saltHex.fromHex() ?: return false
        val actual = digest(salt, candidate).toHex()
        return constantTimeEquals(actual, expectedHashHex)
    }

    private fun digest(salt: ByteArray, password: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(password.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    companion object {
        const val SALT_BYTES = 16
        const val PASSWORD_LENGTH = 8

        /** True if [value] is exactly 8 numeric digits. */
        fun isValidPassword(value: String): Boolean =
            value.length == PASSWORD_LENGTH && value.all { it.isDigit() }

        private fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }

        private fun String.fromHex(): ByteArray? {
            if (length % 2 != 0) return null
            return try {
                ByteArray(length / 2) { i ->
                    substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
            } catch (_: NumberFormatException) {
                null
            }
        }

        /** Length-constant comparison to avoid leaking match position via timing. */
        private fun constantTimeEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var result = 0
            for (i in a.indices) {
                result = result or (a[i].code xor b[i].code)
            }
            return result == 0
        }
    }
}
