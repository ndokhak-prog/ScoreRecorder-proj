package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun todayStored_has8Digits() {
        val s = DateTimeUtils.todayStored()
        assertEquals(8, s.length)
        assertTrue(s.all { it.isDigit() })
    }

    @Test
    fun toDisplay_formats8Digits() {
        assertEquals("2024-09-22", DateTimeUtils.toDisplay("20240922"))
    }

    @Test
    fun toDisplay_passesThroughOtherFormat() {
        assertEquals("24-9", DateTimeUtils.toDisplay("24-9"))
        assertEquals("", DateTimeUtils.toDisplay(""))
    }

    @Test
    fun toStored_removesDashes() {
        assertEquals("20240922", DateTimeUtils.toStored("2024-09-22"))
    }

    @Test
    fun yearOf_extractsYear() {
        assertEquals("2024", DateTimeUtils.yearOf("20240922"))
        assertEquals("1999", DateTimeUtils.yearOf("19991231"))
    }

    @Test
    fun addMonths_crossesYearBoundary() {
        assertEquals("202312", DateTimeUtils.addMonths("202401", -1))
        assertEquals("202501", DateTimeUtils.addMonths("202412", 1))
        assertEquals("202401", DateTimeUtils.addMonths("202312", 1))
        assertEquals("202307", DateTimeUtils.addMonths("202401", -6))
    }

    @Test
    fun isPastOrCurrent_comparesToNow() {
        val now = DateTimeUtils.currentYearMonth()
        assertTrue(DateTimeUtils.isPastOrCurrent(now))
        assertTrue(DateTimeUtils.isPastOrCurrent("190001"))
        assertFalse(DateTimeUtils.isPastOrCurrent("999912"))
    }

    @Test
    fun parseYmDisplay_formats6Digits() {
        assertEquals("2024-01", DateTimeUtils.parseYmDisplay("202401"))
        assertEquals("2024-01", DateTimeUtils.parseYmDisplay("2024-01"))
    }

    @Test
    fun currentYearMonth_is6Digits() {
        assertEquals(6, DateTimeUtils.currentYearMonth().length)
    }

    @Test
    fun millisRoundtrip_withUtc() {
        val millis = DateTimeUtils.storedToMillis("20240922")
        assertNotNull(millis)
        assertEquals("20240922", DateTimeUtils.millisToStored(millis!!))
    }

    @Test
    fun storedToMillis_badInputReturnsNull() {
        assertEquals(null, DateTimeUtils.storedToMillis(""))
        assertEquals(null, DateTimeUtils.storedToMillis("abcd"))
    }

    @Test
    fun toDisplayMd_dropsYear() {
        assertEquals("09-22", DateTimeUtils.toDisplayMd("20240922"))
        assertEquals("24-9", DateTimeUtils.toDisplayMd("24-9"))
    }

    @Test
    fun recent3MonthsFr_isTwoMonthsBeforeCurrent() {
        val cur = DateTimeUtils.currentYearMonth()
        assertEquals(DateTimeUtils.addMonths(cur, -2), DateTimeUtils.recent3MonthsFr())
        assertTrue(DateTimeUtils.isPastOrCurrent(DateTimeUtils.recent3MonthsFr()))
    }
}