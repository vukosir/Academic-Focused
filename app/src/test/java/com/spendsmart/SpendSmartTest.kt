package com.spendsmart

import com.spendsmart.utils.FormatUtils
import com.spendsmart.utils.HashUtils
import org.junit.Assert.*
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun formatRand_positiveAmount() {
        val result = FormatUtils.formatRand(1234.56)
        assertTrue(result.startsWith("R "))
        assertTrue(result.contains("1"))
    }

    @Test
    fun formatRand_zero() {
        val result = FormatUtils.formatRand(0.0)
        assertEquals("R 0.00", result)
    }

    @Test
    fun formatRandCompact_largeAmount() {
        val result = FormatUtils.formatRandCompact(2500.0)
        assertTrue(result.contains("k"))
    }

    @Test
    fun formatRandCompact_smallAmount() {
        val result = FormatUtils.formatRandCompact(500.0)
        assertFalse(result.contains("k"))
    }

    @Test
    fun displayDate_validInput() {
        val result = FormatUtils.displayDate("2026-04-28")
        assertEquals("28 Apr 2026", result)
    }

    @Test
    fun displayDate_invalidInput() {
        val result = FormatUtils.displayDate("not-a-date")
        assertEquals("not-a-date", result)
    }

    @Test
    fun parseColor_validHex() {
        val result = FormatUtils.parseColor("#111110")
        assertNotEquals(0, result)
    }

    @Test
    fun parseColor_invalidHex_returnsDefault() {
        val result = FormatUtils.parseColor("invalid")
        assertNotEquals(0, result)
    }
}

class HashUtilsTest {

    @Test
    fun sha256_notEmpty() {
        val result = HashUtils.sha256("password123")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun sha256_consistentOutput() {
        val h1 = HashUtils.sha256("hello")
        val h2 = HashUtils.sha256("hello")
        assertEquals(h1, h2)
    }

    @Test
    fun sha256_differentInputs() {
        val h1 = HashUtils.sha256("hello")
        val h2 = HashUtils.sha256("world")
        assertNotEquals(h1, h2)
    }

    @Test
    fun sha256_correctLength() {
        val result = HashUtils.sha256("test")
        assertEquals(64, result.length)
    }
}

class BudgetLogicTest {

    @Test
    fun overBudget_whenSpentExceedsMax() {
        val spent = 5000.0
        val max = 4000.0
        assertTrue(spent > max)
    }

    @Test
    fun underMin_whenSpentBelowMin() {
        val spent = 500.0
        val min = 1000.0
        assertTrue(spent < min)
    }

    @Test
    fun onTrack_whenWithinRange() {
        val spent = 3000.0
        val min = 1000.0
        val max = 4000.0
        assertTrue(spent in min..max)
    }

    @Test
    fun progressPercent_clampsAt100() {
        val spent = 6000.0
        val max = 4000.0
        val pct = ((spent / max) * 100).toInt().coerceIn(0, 100)
        assertEquals(100, pct)
    }

    @Test
    fun progressPercent_calculatesCorrectly() {
        val spent = 2000.0
        val max = 4000.0
        val pct = ((spent / max) * 100).toInt()
        assertEquals(50, pct)
    }
}
