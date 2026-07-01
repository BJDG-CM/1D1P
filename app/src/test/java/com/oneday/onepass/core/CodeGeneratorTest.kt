package com.oneday.onepass.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class CodeGeneratorTest {

    private val generator = CodeGenerator(SecureRandom())

    @Test
    fun `generated int is always within 0 to 9999`() {
        repeat(50_000) {
            val value = generator.generateInt()
            assertTrue("value $value out of range", value in 0..9999)
        }
    }

    @Test
    fun `generated code is always exactly 4 numeric digits`() {
        repeat(50_000) {
            val code = generator.generateCode()
            assertEquals("code '$code' not length 4", 4, code.length)
            assertTrue("code '$code' has non-digit", code.all { it.isDigit() })
        }
    }

    @Test
    fun `format zero-pads correctly across the range`() {
        assertEquals("0000", CodeGenerator.format(0))
        assertEquals("0007", CodeGenerator.format(7))
        assertEquals("0042", CodeGenerator.format(42))
        assertEquals("0821", CodeGenerator.format(821))
        assertEquals("4821", CodeGenerator.format(4821))
        assertEquals("9999", CodeGenerator.format(9999))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `format rejects out-of-range values`() {
        CodeGenerator.format(10_000)
    }

    @Test
    fun `codes are not constant - randomness produces many distinct values`() {
        // Not date/seed derived: a large sample must contain many distinct codes.
        val samples = (1..2_000).map { generator.generateCode() }.toSet()
        assertTrue(
            "expected many distinct codes but got ${samples.size}",
            samples.size > 100,
        )
    }

    @Test
    fun `two generators do not lock-step to the same sequence`() {
        val a = CodeGenerator(SecureRandom())
        val b = CodeGenerator(SecureRandom())
        val seqA = (1..20).map { a.generateInt() }
        val seqB = (1..20).map { b.generateInt() }
        // Astronomically unlikely for two SecureRandom sequences of 20 values to be identical.
        assertNotEquals(seqA, seqB)
    }
}
