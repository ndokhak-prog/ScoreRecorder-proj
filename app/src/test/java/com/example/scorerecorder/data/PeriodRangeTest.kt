package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 최근 조회기간 초기값 규칙 (초기화면 / 기록 목록 공통)
 */
class PeriodRangeTest {

    /** 최근3개월 모드 : 현재월 기준 3개월 */
    @Test
    fun recent3MonthsMode() {
        val s = GolfSettings(latestPeriodMode = 1, latestPeriodFr = "202001", latestPeriodTo = "202001")
        assertEquals("202409" to "202411", s.latestPeriodRange("202411"))
    }

    /** 기간 모드 : 설정에 저장된 기간을 그대로 사용 */
    @Test
    fun fixedPeriodMode() {
        val s = GolfSettings(latestPeriodMode = 0, latestPeriodFr = "202401", latestPeriodTo = "202406")
        assertEquals("202401" to "202406", s.latestPeriodRange("202411"))
    }

    /** 기간이 비어 있으면 현재월 */
    @Test
    fun emptyPeriodFallsBackToCurrentMonth() {
        val s = GolfSettings(latestPeriodMode = 0, latestPeriodFr = "", latestPeriodTo = "")
        assertEquals("202411" to "202411", s.latestPeriodRange("202411"))
    }

    /** 연도 경계 : 2025-02 의 3개월 전은 2024-12 */
    @Test
    fun recent3MonthsAcrossYearBoundary() {
        val s = GolfSettings(latestPeriodMode = 1)
        assertEquals("202412" to "202502", s.latestPeriodRange("202502"))
    }
}
