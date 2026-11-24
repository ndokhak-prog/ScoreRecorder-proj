package com.example.scorerecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** 점수입력창 상단 정보(홀/기준타/점수방식/선수명) 표시 테스트 */
class ScorePopupInfoTest {

    @Test
    fun absolute_showsAllInfo() {
        assertEquals(
            "홀 B-3  |  기준타 5  |  절대점수(A)  |  선수 홍길동 프로",
            scorePopupInfo("B-3", 5, "A", "홍길동 프로")
        )
    }

    @Test
    fun relative_showsRelativeType() {
        assertEquals(
            "홀 A-1  |  기준타 4  |  상대점수(R)  |  선수 김철수 프로",
            scorePopupInfo("A-1", 4, "R", "김철수 프로")
        )
    }

    @Test
    fun missingValues_areOmitted_butScoreTypeAlwaysShown() {
        assertEquals("기준타 -  |  절대점수(A)", scorePopupInfo(null, null, "A", null))
        assertEquals("기준타 3  |  상대점수(R)", scorePopupInfo("  ", 3, "R", ""))
    }
}
