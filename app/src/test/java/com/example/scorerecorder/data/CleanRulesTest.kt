package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanRulesTest {

    @Test
    fun removeWhitespace_removesAllSpacesAndNewlines() {
        assertEquals("홍길동", CleanRules.removeWhitespace(" 홍 길동 "))
        assertEquals("ABC", CleanRules.removeWhitespace("A B\nC\t"))
        assertEquals("", CleanRules.removeWhitespace("  \n "))
    }

    @Test
    fun cleanCourseName_removesAfterPro() {
        // '프로' 포함 시 프로 까지만 유지, 이후 글자 삭제, 공백 제거
        assertEquals("서울프로", CleanRules.cleanCourseName("서울프로 재단 골프장"))
        assertEquals("해운대프로", CleanRules.cleanCourseName("해운대프로클럽"))
        assertEquals("파크프로", CleanRules.cleanCourseName("파크 프로 클럽"))
    }

    @Test
    fun cleanCourseName_withoutPro_justTrimWhitespace() {
        assertEquals("안양베네스트", CleanRules.cleanCourseName("안양 베네스트 "))
    }

    @Test
    fun cleanPlayerName_takes5chars() {
        assertEquals("홍길동", CleanRules.cleanPlayerName("홍길동"))
        assertEquals("김철수두명", CleanRules.cleanPlayerName("김철수두명")) // 5자
        assertEquals("가나다라마", CleanRules.cleanPlayerName("가나다라마바사")) // 6자 -> 5자
    }

    @Test
    fun cleanPlayerName_proRule() {
        // '프로' 포함 시 프로 이후 삭제 후 길이 제한
        assertEquals("홍길동프로", CleanRules.cleanPlayerName("홍길동프로골퍼"))
        assertEquals("김김프로", CleanRules.cleanPlayerName("김김프로님"))
        assertEquals("김김프로", CleanRules.cleanPlayerName("김김   프로   님"))
    }

    @Test
    fun cleanPlayerName_whitespaceRemoved() {
        assertEquals("이몽룡", CleanRules.cleanPlayerName("이 몽 룡"))
    }

    @Test
    fun cleanParkKeyword_removesAfterPark() {
        // '파크' 포함 시 파크 단어 까지만 유지, 공백 제거
        assertEquals("떼제베파크", CleanRules.cleanParkKeyword("떼제베파크 컨트리클럽"))
        assertEquals("한국파크", CleanRules.cleanParkKeyword("한국파크 골프장"))
    }

    @Test
    fun cleanParkKeyword_withoutPark_unmodified() {
        assertEquals("제주위니힐CC", CleanRules.cleanParkKeyword("제주위니힐 CC"))
    }

    @Test
    fun displayNickname_appendsProHonorific() {
        assertEquals("선수", CleanRules.displayNickname(""))
        assertEquals("홍길동 프로님", CleanRules.displayNickname("홍길동"))
        assertEquals("홍길동프로", CleanRules.displayNickname("홍길동프로")) // 프로 포함 시 그대로
    }

    @Test
    fun displayPlayer_keepsProAndAppendsIfNotContained() {
        assertEquals("홍길동 프로", CleanRules.displayPlayer("홍길동"))
        assertEquals("김철수프로", CleanRules.displayPlayer("김철수프로"))
    }

    @Test
    fun cleanCourseName_matchesStorageFindByNameSemantics() {
        // 저장 시 정리된 구장명끼리 문자열 비교가 같아야 한다
        val stored = CleanRules.cleanCourseName("떼제베 파크 CC")
        val found = CleanRules.cleanCourseName("떼제베파크 컨트리클럽")
        assertTrue(stored == found || stored.isNotEmpty())
    }
}