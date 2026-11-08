package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreEngineTest {

    @Test
    fun courseCount_roundsUpPer9Holes() {
        assertEquals(1, ScoreEngine.courseCount(1))
        assertEquals(1, ScoreEngine.courseCount(9))
        assertEquals(2, ScoreEngine.courseCount(10))
        assertEquals(2, ScoreEngine.courseCount(18))
        assertEquals(3, ScoreEngine.courseCount(19))
        assertEquals(3, ScoreEngine.courseCount(27))
        assertEquals(4, ScoreEngine.courseCount(28))
        assertEquals(4, ScoreEngine.courseCount(36))
        assertEquals(5, ScoreEngine.courseCount(37))
    }

    @Test
    fun courseCount_zero() {
        assertEquals(0, ScoreEngine.courseCount(0))
    }

    @Test
    fun playerTotal_absolute_sumsFilled() {
        assertEquals(45, ScoreEngine.playerTotal(listOf(5, 5, 5, 5, 5, 5, 5, 5, 5), 0, "A"))
        assertEquals(43, ScoreEngine.playerTotal(listOf(5, 6, 4, null, 5, 5, 5, 5, 8), 0, "A"))
    }

    @Test
    fun playerTotal_relative_addsBasePar() {
        val scores = listOf(0, -1, +1) // 상대점수 가감
        assertEquals(36 + 0, ScoreEngine.playerTotal(scores, 36, "R"))
        assertEquals(36 - 1, ScoreEngine.playerTotal(listOf(0, -1), 36, "R"))
        assertEquals(36 + 1, ScoreEngine.playerTotal(listOf(0, null, 1), 36, "R"))
    }

    @Test
    fun playerTotal_relative_ignoresNull() {
        // null 입력 칸은 합계에 영향 없음
        assertEquals(36, ScoreEngine.playerTotal(listOf(null, null), 36, "R"))
        assertEquals(0, ScoreEngine.playerTotal(listOf(null, null), 0, "A"))
    }

    @Test
    fun rankAscending_allComplete() {
        // 3명 모두 완주: 순위는 총타 오름차순
        val a = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1) // 합 9
        val b = listOf(1, 1, 1, 1, 1, 1, 1, 1, 2) // 합 10
        val c = listOf(1, 1, 1, 1, 1, 1, 1, 1, 0) // 합 8
        val ranks = ScoreEngine.computeRanks(listOf(a, b, c), requiredHoles = 9, baseParAll = 0, scoreType = "A")
        assertEquals(listOf(2, 3, 1), ranks)
    }

    @Test
    fun rank_excludesIncompletePlayers() {
        // 3명중 1명(마지막)이 홀 하나 미입력 → 순위 제외(0)
        val a = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1)   // 완주, 합 9
        val b = listOf(1, 1, 1, 1, 1, 1, 1, 1, 2)   // 완주, 합 10
        val c = listOf(1, 1, 1, 1, 1, 1, 1, 1, null) // 미완
        val ranks = ScoreEngine.computeRanks(listOf(a, b, c), requiredHoles = 9, baseParAll = 0, scoreType = "A")
        assertEquals(listOf(1, 2, 0), ranks)
    }

    @Test
    fun rank_tie_getsSameRank() {
        // 동일 총타(둘 다 4타)면 동순위(1,1,3)
        val a = listOf(0, 0, 0, 0, 0, 0, 0, 0, 4) // 합 4
        val b = listOf(1, 1, 1, 1, 0, 0, 0, 0, 0) // 합 4
        val c = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1) // 합 9
        val ranks = ScoreEngine.computeRanks(listOf(a, b, c), requiredHoles = 9, baseParAll = 0, scoreType = "A")
        assertEquals(listOf(1, 1, 3), ranks)
    }

    @Test
    fun competitionRanks_example_1_2_2_4() {
        // 예시) 1, 2, 2, 4 (컴페티션 방식)
        assertEquals(listOf(1, 2, 2, 4), ScoreEngine.competitionRanks(listOf(70, 75, 75, 80)))
    }

    @Test
    fun competitionRanks_noTie() {
        assertEquals(listOf(1, 2, 3), ScoreEngine.competitionRanks(listOf(70, 72, 80)))
    }

    @Test
    fun competitionRanks_allTied() {
        assertEquals(listOf(1, 1, 1), ScoreEngine.competitionRanks(listOf(75, 75, 75)))
    }

    @Test
    fun rank_relativeScoringWithBasePar() {
        // R타입: 기준타 36 + 가감
        val a = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0) // 36
        val b = listOf(-1, 0, 0, 0, 0, 0, 0, 0, 0) // 35
        val c = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1)  // 45
        val ranks = ScoreEngine.computeRanks(listOf(a, b, c), requiredHoles = 9, baseParAll = 36, scoreType = "R")
        assertEquals(listOf(2, 1, 3), ranks)
    }

    @Test
    fun computeTotals_matchesPlayerTotalPerType() {
        val players = listOf(
            listOf(5, 5, 5, 5, 5, 5, 5, 5, 5),
            listOf(5, 5, 5, 5, 5, 5, 5, 5, 4)
        )
        assertEquals(listOf(45, 44), ScoreEngine.computeTotals(players, 0, "A"))
        assertEquals(listOf(81, 80), ScoreEngine.computeTotals(players, 36, "R"))
    }
}