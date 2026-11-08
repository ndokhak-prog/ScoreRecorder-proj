package com.example.scorerecorder.ui

import androidx.compose.runtime.mutableStateListOf
import com.example.scorerecorder.data.PlayerScore
import com.example.scorerecorder.data.ScoreRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 기록상세 저장 로직: 미입력(null)은 상대점수(R)/절대점수(A) 모두 null 로 저장되어
 * 다시 열었을 때 '-' 로 표시되어야 한다.
 */
class RecordDetailPersistTest {

    private val pars = listOf(4, 5, 3, 4, 5, 4, 3, 5, 4)

    private fun record(scoreType: String) = ScoreRecord(
        gameId = "20261108001",
        courseName = "테스트구장",
        gameDate = "20261108",
        region = "서울",
        totalHoles = 9,
        distances = pars.map { it * 100 },
        pars = pars,
        scoreType = scoreType,
        players = mutableListOf(PlayerScore(name = "홍길동"))
    )

    /** 상대점수(R): 입력한 0(파)은 유지, 미입력은 null 유지 */
    @Test
    fun relative_keeps_null_for_unentered_holes() {
        val scores = mutableStateListOf<Int?>(0, 1, null, null, null, null, null, null, null)
        val out = buildUpdatedRecord(
            record("R"), pars, pars, listOf(PlayerEdit("홍길동", scores)), "R", 9, "20261108"
        )
        val s = out.players[0].scores

        assertEquals(0, s[0] ?: -999)     // 입력한 0(파) 유지
        assertEquals(1, s[1] ?: -999)
        assertNull(s[2])                  // 미입력은 null
        assertEquals(10, s.size)          // 홀 9 + 합계 1
        // 상대점수 합계 = 기준타 합계 + 입력값 합계
        assertEquals(pars.sum() + 1, out.players[0].total)
        assertEquals(out.players[0].total, s.last() ?: -999)
    }

    /** 절대점수(A): 미입력은 null 유지 */
    @Test
    fun absolute_keeps_null_for_unentered_holes() {
        val scores = mutableStateListOf<Int?>(4, 6, null, null, null, null, null, null, null)
        val out = buildUpdatedRecord(
            record("A"), pars, pars, listOf(PlayerEdit("홍길동", scores)), "A", 9, "20261108"
        )
        val s = out.players[0].scores

        assertEquals(10, s.dropLast(1).filterNotNull().sum())
        assertNull(s[2])
        assertEquals(10, out.players[0].total)
    }

    /** 한 홀도 입력하지 않으면 전부 null (기록상세 '-' 표시) */
    @Test
    fun all_unentered_stays_null() {
        val scores = mutableStateListOf<Int?>(*arrayOfNulls<Int?>(9))
        val out = buildUpdatedRecord(
            record("R"), pars, pars, listOf(PlayerEdit("홍길동", scores)), "R", 9, "20261108"
        )
        val s = out.players[0].scores

        assertEquals(9, s.dropLast(1).count { it == null })
        assertEquals(pars.sum(), out.players[0].total)  // 상대점수는 기준타 합계
    }
}
