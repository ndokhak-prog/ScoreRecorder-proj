package com.example.scorerecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 거리(m) → 기준타 자동계산 로직 검증
 *  - 공백(null)/0 → 0
 *  - 1~60 → 3
 *  - 61~109 → 4
 *  - 110 이상 → 5
 */
class ParFromDistanceTest {

    @Test
    fun blankOrZero_isZero() {
        assertEquals(0, parForDistance(null))
        assertEquals(0, parForDistance(0))
        assertEquals(0, parForDistance(-5))
    }

    @Test
    fun upTo60_isPar3() {
        assertEquals(3, parForDistance(1))
        assertEquals(3, parForDistance(60))
    }

    @Test
    fun from61To109_isPar4() {
        assertEquals(4, parForDistance(61))
        assertEquals(4, parForDistance(109))
    }

    @Test
    fun from110_isPar5() {
        assertEquals(5, parForDistance(110))
        assertEquals(5, parForDistance(300))
    }
}
