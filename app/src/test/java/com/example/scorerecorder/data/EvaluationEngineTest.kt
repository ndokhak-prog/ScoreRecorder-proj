package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 구장평가 순수 로직 단위테스트
 *  - 평균: 입력된 항목만 계산 (미입력 항목 제외)
 *  - 별 반올림: 0.5 단위 (0.25↑0.75↓ → 0.5, 0.75↑1.25↓ → 1.0)
 *  - 점수 파싱: 0~5.0 범위, 0.5 단위 정규화
 */
class EvaluationEngineTest {

    // ---------- average ----------

    @Test
    fun average_allNull_returnsNull() {
        assertNull(EvaluationEngine.average(List(9) { null }))
    }

    @Test
    fun average_onlyEnteredItems() {
        // 4.0, 4.5 입력 → 평균 4.25 (나머지 7개 미입력 무시)
        val scores = MutableList<Float?>(9) { null }
        scores[0] = 4.0f
        scores[3] = 4.5f
        assertEquals(4.25f, EvaluationEngine.average(scores)!!, 0.001f)
    }

    @Test
    fun average_allEntered() {
        val scores = MutableList<Float?>(9) { 4.0f }
        assertEquals(4.0f, EvaluationEngine.average(scores)!!, 0.001f)
    }

    // ---------- starRound (별 0.5 단위 반올림) ----------

    @Test
    fun starRound_halfStarRange() {
        // 0.25 이상 0.75 미만 → 0.5 (별 반)
        assertEquals(0.5f, EvaluationEngine.starRound(0.25f), 0.001f)
        assertEquals(0.5f, EvaluationEngine.starRound(0.5f), 0.001f)
        assertEquals(0.5f, EvaluationEngine.starRound(0.74f), 0.001f)
    }

    @Test
    fun starRound_oneStarRange() {
        // 0.75 이상 1.25 미만 → 1.0 (별 하나)
        assertEquals(1.0f, EvaluationEngine.starRound(0.75f), 0.001f)
        assertEquals(1.0f, EvaluationEngine.starRound(1.0f), 0.001f)
        assertEquals(1.0f, EvaluationEngine.starRound(1.24f), 0.001f)
    }

    @Test
    fun starRound_fiveStars() {
        assertEquals(5.0f, EvaluationEngine.starRound(4.8f), 0.001f)
        assertEquals(5.0f, EvaluationEngine.starRound(5.0f), 0.001f)
    }

    // ---------- snapScore (0.5 단위 정규화) ----------

    @Test
    fun snapScore_roundsToHalfStep() {
        assertEquals(3.5f, EvaluationEngine.snapScore(3.3f), 0.001f)
        assertEquals(3.5f, EvaluationEngine.snapScore(3.7f), 0.001f)
        assertEquals(4.0f, EvaluationEngine.snapScore(3.8f), 0.001f)
    }

    @Test
    fun snapScore_clampsRange() {
        assertEquals(0f, EvaluationEngine.snapScore(-1f), 0.001f)
        assertEquals(5f, EvaluationEngine.snapScore(99f), 0.001f)
    }

    // ---------- parseScoreInput ----------

    @Test
    fun parseScoreInput_validValues() {
        assertEquals(4.0f, EvaluationEngine.parseScoreInput("4")!!, 0.001f)
        assertEquals(4.5f, EvaluationEngine.parseScoreInput("4.5")!!, 0.001f)
        assertEquals(0f, EvaluationEngine.parseScoreInput("0")!!, 0.001f)
        assertEquals(5f, EvaluationEngine.parseScoreInput("5.0")!!, 0.001f)
    }

    @Test
    fun parseScoreInput_emptyOrNull() {
        assertNull(EvaluationEngine.parseScoreInput(""))
        assertNull(EvaluationEngine.parseScoreInput("  "))
    }

    @Test
    fun parseScoreInput_invalidReturnsNull() {
        assertNull(EvaluationEngine.parseScoreInput("abc"))
        assertNull(EvaluationEngine.parseScoreInput("7"))    // 범위 밖
        assertNull(EvaluationEngine.parseScoreInput("-1"))   // 범위 밖
    }

    @Test
    fun parseScoreInput_offStepSnapsToHalf() {
        // 0.5 단위가 아니면 반올림 정규화
        assertEquals(3.5f, EvaluationEngine.parseScoreInput("3.3")!!, 0.001f)
    }

    // ---------- stepText (증감버튼) ----------

    @Test
    fun stepText_plusHalf() {
        assertEquals("4.5", EvaluationEngine.stepText("4", 0.5f))
        assertEquals("0.5", EvaluationEngine.stepText("", 0.5f))   // 빈값 → 0 기준
    }

    @Test
    fun stepText_minusHalf() {
        assertEquals("3.5", EvaluationEngine.stepText("4", -0.5f))
    }

    @Test
    fun stepText_clampsAtBounds() {
        assertEquals("5.0", EvaluationEngine.stepText("5", 0.5f))
        assertEquals("0.0", EvaluationEngine.stepText("0", -0.5f))
    }
}
