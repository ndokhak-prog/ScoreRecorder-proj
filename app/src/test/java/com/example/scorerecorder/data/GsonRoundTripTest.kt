package com.example.scorerecorder.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JSON(Gson) 직렬화/역직렬화 왕복 테스트.
 * 앱은 ndokhak_*.json 저장에 Gson을 사용하므로 모델이 왕복에서 손실되지 않아야 한다.
 */
class GsonRoundTripTest {

    private val gson = Gson()

    @Test
    fun scoreRecord_roundTrip() {
        val record = ScoreRecord(
            gameId = "2024ABC123",
            courseName = "떼제베",
            gameDate = "20240922",
            region = "경기",
            totalHoles = 9,
            distances = listOf(480, 170, 330, 520, 400, 160, 350, 450, 210, 3070),
            pars = listOf(5, 3, 4, 5, 4, 3, 4, 4, 3, 35),
            scoreType = "R",
            players = mutableListOf(
                PlayerScore("홍길동", mutableListOf(5, 3, 4, 5, 4, 3, 4, 4, 3, 35), 35, 1, false),
                PlayerScore("김철수", mutableListOf(5, 4, 4, 5, 4, 3, 4, 4, 4, 37), 37, 2, false)
            ),
            updateDate = "2024-09-22 18:00:00"
        )

        val json = gson.toJson(record)
        val type = object : TypeToken<ScoreRecord>() {}.type
        val restored = gson.fromJson<ScoreRecord>(json, type)

        assertEquals(record, restored)
        assertEquals(2, restored.players.size)
        assertEquals("홍길동", restored.players[0].name)
        assertEquals(35, restored.players[0].total)
    }

    @Test
    fun scoreRecord_withNullScoresRoundTrip() {
        // 미입력 칸(null)이 왕복에서 유지되어야 함
        val record = ScoreRecord(
            gameId = "2024XYZ",
            courseName = "파크",
            gameDate = "20240923",
            totalHoles = 9,
            players = mutableListOf(
                PlayerScore("선수1", mutableListOf(5, null, null, 3, null, null, null, null, null, 8), 8, 0, true)
            )
        )

        val json = gson.toJson(record)
        val type = object : TypeToken<ScoreRecord>() {}.type
        val restored = gson.fromJson<ScoreRecord>(json, type)

        assertEquals(record, restored)
        assertNull(restored.players[0].scores[1])
        assertEquals(8, restored.players[0].scores[9])
        assertTrue(restored.players[0].excluded)
    }

    @Test
    fun golfCourse_roundTrip_withCourses() {
        val course = GolfCourse(
            courseId = "P1",
            region = "제주",
            courseName = "제주특별자치도클럽",
            totalHoles = 18,
            courses = listOf(
                HoleCourse("A", listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 45), listOf(4, 4, 4, 4, 5, 4, 4, 4, 4, 36)),
                HoleCourse("B", listOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 45), listOf(4, 4, 4, 4, 4, 4, 4, 3, 3, 34))
            ),
            location = "제주시 애월읍",
            openTime = "06:00~18:00",
            extraInfo = "회원제"
        )

        val type = object : TypeToken<GolfCourse>() {}.type
        val restored = gson.fromJson<GolfCourse>(gson.toJson(course), type)

        assertEquals(course, restored)
        assertEquals(2, restored.courses.size)
        assertEquals("B", restored.courses[1].code)
        assertEquals(34, restored.courses[1].pars.last())
    }

    @Test
    fun golfSettings_roundTrip() {
        val settings = GolfSettings(
            settingId = "S1",
            nickname = "홍길동",
            nicknames = listOf("홍길동", "김철수", "박영희"),
            latestViewType = 1,
            latestPeriodFr = "202401",
            latestPeriodTo = "202409",
            latestPlayerName = "김철수",
            newHole = 27,
            scoreType = "R",
            playerCount = 4,
            courseQueryType = "구장명",
            region = "경기",
            updateDate = "2024-09-22 10:00:00"
        )

        val type = object : TypeToken<GolfSettings>() {}.type
        val restored = gson.fromJson<GolfSettings>(gson.toJson(settings), type)

        assertEquals(settings, restored)
    }

    @Test
    fun emptyPlayersList_roundTrip() {
        val record = ScoreRecord(gameId = "X1", courseName = "신규", gameDate = "20240924", totalHoles = 9)
        val type = object : TypeToken<ScoreRecord>() {}.type
        val restored = gson.fromJson<ScoreRecord>(gson.toJson(record), type)

        assertEquals(record, restored)
        assertTrue(restored.players.isEmpty())
    }
}