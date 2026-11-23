package com.example.scorerecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 홀 점수 음성인식 파싱 테스트
 */
class VoiceScoreInputTest {

    @Test
    fun parseVoiceScore_readsArabicDigits() {
        assertEquals(4, parseVoiceScore("4"))
        assertEquals(0, parseVoiceScore("0"))
        assertEquals(12, parseVoiceScore("12"))
    }

    @Test
    fun parseVoiceScore_readsKoreanNumberWords() {
        assertEquals(4, parseVoiceScore("사"))
        assertEquals(1, parseVoiceScore("일"))
        assertEquals(2, parseVoiceScore("이"))
        assertEquals(3, parseVoiceScore("삼"))
        assertEquals(5, parseVoiceScore("오"))
        assertEquals(9, parseVoiceScore("구"))
    }

    @Test
    fun parseVoiceScore_readsTaePrefix() {
        assertEquals(4, parseVoiceScore("타이 4"))
        assertEquals(3, parseVoiceScore("타이3"))
    }

    @Test
    fun parseVoiceScore_handlesMinusWords() {
        assertEquals(-1, parseVoiceScore("마이너스 1"))
        assertEquals(-2, parseVoiceScore("빼 2"))
        assertEquals(-3, parseVoiceScore("-3"))
        assertEquals(-4, parseVoiceScore("마이너스 사"))
    }

    @Test
    fun parseVoiceScore_handlesPlus() {
        assertEquals(1, parseVoiceScore("+1"))
        assertEquals(2, parseVoiceScore("플러스 2"))
    }

    @Test
    fun parseVoiceScore_readsGolfTerms() {
        assertEquals(-1, parseVoiceScore("버디"))
        assertEquals(-2, parseVoiceScore("보리"))
        assertEquals(-3, parseVoiceScore("이글"))
        assertEquals(-4, parseVoiceScore("알바트로스"))
        assertEquals(0, parseVoiceScore("파"))
        assertEquals(0, parseVoiceScore("제로"))
    }

    @Test
    fun parseVoiceScore_readsEnglishDigits() {
        assertEquals(4, parseVoiceScore("four"))
        assertEquals(3, parseVoiceScore("three"))
        assertEquals(0, parseVoiceScore("zero"))
    }

    @Test
    fun parseVoiceScore_returnsNullWhenNotANumber() {
        assertNull(parseVoiceScore(""))
        assertNull(parseVoiceScore("   "))
        assertNull(parseVoiceScore("안녕하세요"))
        assertNull(parseVoiceScore("그럴 수도 있을 거예요"))
    }

    @Test
    fun parseVoiceScore_readsAdditionalSynonyms() {
        assertEquals(2, parseVoiceScore("투"))
        assertEquals(1, parseVoiceScore("원"))
        assertEquals(4, parseVoiceScore("포"))
        assertEquals(5, parseVoiceScore("파이브"))
        assertEquals(-3, parseVoiceScore("에이스"))
        assertEquals(-4, parseVoiceScore("도전"))
        assertEquals(4, parseVoiceScore("4점"))
        assertEquals(4, parseVoiceScore("4 타"))
    }

    @Test
    fun pickVoiceScore_picksFirstCandidateInRange() {
        val r = pickVoiceScore(listOf("파", "4", "3"), listOf(-2, -1, 1, 2, 3, 4))
        assertEquals(AcceptResult.ACCEPTED, r.first)
        assertEquals(4, r.second)
    }

    @Test
    fun pickVoiceScore_reportsOutOfRange() {
        val r = pickVoiceScore(listOf("9"), listOf(0, 1, 2, 3, 4))
        assertEquals(AcceptResult.OUT_OF_RANGE, r.first)
        assertEquals(9, r.second)
    }

    @Test
    fun pickVoiceScore_reportsUnparsedWhenNoCandidateIsNumber() {
        val r = pickVoiceScore(listOf("안녕하세요", "어쨌죠"), listOf(1, 2, 3))
        assertEquals(AcceptResult.UNPARSED, r.first)
    }

    // ---- 실제 인식 결과(조사/말투 포함)와 절대점수 환산 ----

    @Test
    fun resolve_absolute_convertsGolfTermsUsingPar() {
        val allowed = (1..10).toList()
        assertEquals(4, resolveVoiceScore(listOf("버디"), allowed, "A", 5).second)
        assertEquals(5, resolveVoiceScore(listOf("파"), allowed, "A", 5).second)
        assertEquals(6, resolveVoiceScore(listOf("보기"), allowed, "A", 5).second)
        assertEquals(3, resolveVoiceScore(listOf("이글"), allowed, "A", 5).second)
        assertEquals(AcceptResult.ACCEPTED, resolveVoiceScore(listOf("버디"), allowed, "A", 5).first)
    }

    @Test
    fun resolve_relative_keepsMarks() {
        val allowed = (-5..5).toList()
        assertEquals(-1, resolveVoiceScore(listOf("버디"), allowed, "R", 5).second)
        assertEquals(0, resolveVoiceScore(listOf("파"), allowed, "R", 5).second)
        assertEquals(1, resolveVoiceScore(listOf("보기"), allowed, "R", 5).second)
    }

    @Test
    fun resolve_acceptsSpokenParticlesAndColloquialDigits() {
        val allowed = (1..10).toList()
        assertEquals(4, resolveVoiceScore(listOf("사야"), allowed, "A", 4).second)
        assertEquals(4, resolveVoiceScore(listOf("버디예요"), allowed, "A", 5).second)
        assertEquals(4, resolveVoiceScore(listOf("네"), allowed, "A", 4).second)
        assertEquals(AcceptResult.ACCEPTED, resolveVoiceScore(listOf("사", "버디"), allowed, "A", 5).first)
    }

    @Test
    fun resolve_holeInOne_isAbsoluteOne() {
        val allowed = (1..10).toList()
        assertEquals(1, resolveVoiceScore(listOf("홀인원"), allowed, "A", 4).second)
        assertEquals(-3, resolveVoiceScore(listOf("홀인원"), allowed, "R", 4).second)
    }

    @Test
    fun resolve_reportsOutOfRangeAndUnparsed() {
        assertEquals(
            AcceptResult.OUT_OF_RANGE,
            resolveVoiceScore(listOf("보기"), listOf(1, 2, 3), "A", 5).first
        )
        assertEquals(
            AcceptResult.UNPARSED,
            resolveVoiceScore(listOf("안녕하세요"), listOf(1, 2, 3), "A", 5).first
        )
    }
}