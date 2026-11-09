package com.rohan.lockin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeMathAndMoneyTest {

    @Test
    fun `elapsed splits into days hours minutes seconds`() {
        val from = 1_000_000L
        val now = from + ((2 * 86_400L + 3 * 3_600L + 4 * 60L + 5L) * 1_000L)
        assertEquals(Elapsed(2, 3, 4, 5), SmokeMath.elapsed(from, now))
    }

    @Test
    fun `unset quit time shows zero`() {
        assertEquals(Elapsed(0, 0, 0, 0), SmokeMath.elapsed(0L, 99_999_999L))
    }

    @Test
    fun `24h milestone unlocks at exactly 24 hours`() {
        val m = SmokeMath.MILESTONES.first()
        val from = 1_000L
        assertFalse(SmokeMath.isUnlocked(m, from, from + 24 * 3_600_000L - 1))
        assertTrue(SmokeMath.isUnlocked(m, from, from + 24 * 3_600_000L))
        assertEquals(0.5f, SmokeMath.progress(m, from, from + 12 * 3_600_000L), 0.001f)
    }

    @Test
    fun `rupees use indian grouping`() {
        assertEquals("₹4,00,000", rupees(400_000))
        assertEquals("₹999", rupees(999))
        assertEquals("-₹26,500", rupees(-26_500))
        assertEquals("₹1,23,45,678", rupees(12_345_678))
        assertEquals("+₹1,440", signedRupees(1_440))
    }

    @Test
    fun `mmss formats countdowns`() {
        assertEquals("60:00", mmss(3_600_000L))
        assertEquals("00:00", mmss(-5L))
        assertEquals("04:05", mmss(245_000L))
    }
}
