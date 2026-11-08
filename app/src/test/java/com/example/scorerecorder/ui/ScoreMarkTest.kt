package com.example.scorerecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 타수 변환 규칙 단위테스트 (strokeMark)
 */
class ScoreMarkTest {

    @Test
    fun holeInOne() {
        assertEquals("H", strokeMark(1, 4))
        assertEquals("H", strokeMark(1, 5))
    }

    @Test
    fun albatrossEagleBirdiePar() {
        assertEquals("H", strokeMark(1, 4))     // 파4에서 1타는 H 우선
        assertEquals("A", strokeMark(2, 5))     // 파5에서 2타
        assertEquals("E", strokeMark(2, 4))
        assertEquals("B", strokeMark(3, 4))
        assertEquals("P", strokeMark(4, 4))
    }

    @Test
    fun bogeyDoubleBogey() {
        assertEquals("+1", strokeMark(5, 4))
        assertEquals("+2", strokeMark(6, 4))
    }

    @Test
    fun tripleBogey_par4plus() {
        assertEquals("+3", strokeMark(7, 4))
        assertEquals("+3", strokeMark(8, 5))
    }

    @Test
    fun quarterBogey_par5plus() {
        assertEquals("+4", strokeMark(9, 5))
        assertEquals("+4", strokeMark(11, 7))
    }

    @Test
    fun noTripleBelowPar4() {
        assertEquals("5", strokeMark(5, 2))     // 파2 +3 -> 원 타수
        assertEquals("7", strokeMark(7, 3))     // 파3 +4 -> 원 타수
    }

    @Test
    fun doubleOfPar() {
        assertEquals("2P", strokeMark(8, 4))
        assertEquals("2P", strokeMark(10, 5))
        assertEquals("2P", strokeMark(6, 3))
    }

    @Test
    fun plainStrokes() {
        assertEquals("9", strokeMark(9, 4))     // 파4 +5
        assertEquals("8", strokeMark(8, 3))     // 파3 +5
        assertEquals("11", strokeMark(11, 3))   // 파3 2배(6) 이후
    }

    @Test
    fun doubleOfParBeatsTriple() {
        assertEquals("2P", strokeMark(8, 4))    // 파4 8타: +3(par4)보다 2P 우선
    }
}