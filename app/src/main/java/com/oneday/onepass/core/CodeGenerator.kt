package com.oneday.onepass.core

import java.security.SecureRandom

/**
 * Generates the daily 4-digit code.
 *
 * The value is drawn from [SecureRandom] over the full 0..9999 range. It is deliberately NOT
 * derived from the date, a previous code, or any hash — it must be genuinely unpredictable so a
 * code cannot be reconstructed by knowing the day.
 *
 * This class is pure (no Android dependencies) so it can be unit tested directly.
 */
class CodeGenerator(private val random: SecureRandom = SecureRandom()) {

    /**
     * @return an integer in [0, 9999].
     */
    fun generateInt(): Int = random.nextInt(RANGE) // nextInt(10000) -> 0..9999

    /**
     * @return the code as a zero-padded 4-character string, e.g. "0007" or "4821".
     */
    fun generateCode(): String = format(generateInt())

    companion object {
        const val RANGE = 10_000
        const val LENGTH = 4

        /** Zero-pads a raw int into the canonical 4-digit string. */
        fun format(value: Int): String {
            require(value in 0 until RANGE) { "code must be in 0..9999, was $value" }
            return value.toString().padStart(LENGTH, '0')
        }
    }
}
