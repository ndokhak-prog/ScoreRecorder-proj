package com.example.scorerecorder.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * 구장정보 / 경기기록 내보내기·가져오기(병합) 테스트
 */
@RunWith(RobolectricTestRunner::class)
class StorageIoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage = Storage(context)

    private fun scoreFile(year: String) = File(context.filesDir, "ndokhak_score_$year.json")

    @Test
    fun places_exportThenImport_mergesByCourseId() {
        storage.savePlaces(
            listOf(
                GolfCourse(courseId = "C1", courseName = "가"),
                GolfCourse(courseId = "C2", courseName = "나")
            )
        )
        val exported = storage.exportPlacesJson()

        // 현재에는 C1만 남기고 다른 값으로 변경
        storage.savePlaces(listOf(GolfCourse(courseId = "C1", courseName = "가수정")))

        val (added, total) = storage.importPlacesJson(exported)

        assertEquals(1, added)      // C2 신규
        assertEquals(2, total)
        val byId = storage.loadPlaces().associateBy { it.courseId }
        assertEquals("가", byId["C1"]?.courseName)       // 같은 id → 가져온(파일) 값으로 대체
        assertEquals("나", byId["C2"]?.courseName)        // 신규 추가
    }

    @Test
    fun places_importInvalidJson_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.importPlacesJson("{not json}")
        }
    }

    @Test
    fun records_exportThenImport_mergesAcrossYears() {
        storage.saveRecords("2025", listOf(ScoreRecord(gameId = "2025AAAA", courseName = "봄")))
        storage.saveRecords("2026", listOf(ScoreRecord(gameId = "2026BBBB", courseName = "여름")))
        val exported = storage.exportRecordsJson()

        // 2025 년도 파일 삭제 + 2026 항목 수정
        scoreFile("2025").delete()
        storage.saveRecords("2026", listOf(ScoreRecord(gameId = "2026BBBB", courseName = "여름수정")))

        val (added, total) = storage.importRecordsJson(exported)

        assertEquals(1, added)      // 2025AAAA 신규
        assertEquals(2, total)
        assertEquals("봄", storage.loadRecordByYearAND("2025AAAA", "2025")?.courseName)
        assertEquals("여름", storage.loadRecordByYearAND("2026BBBB", "2026")?.courseName)  // 같은 id → 가져온 값으로 대체

        scoreFile("2025").delete()
        scoreFile("2026").delete()
    }

    @Test
    fun records_importSameIdInWrongYearFile_movesWithoutDuplicate() {
        // 2025 년도 prefix의 기록이 실수로 2026 파일에 들어가 있는 상황
        storage.saveRecords("2026", listOf(ScoreRecord(gameId = "2025ZZZZ", courseName = "원본")))

        val (added, total) = storage.importRecordsJson("""[{"gameId":"2025ZZZZ","courseName":"수정본"}]""")

        assertEquals(0, added)     // 이미 존재하는 id → 신규 아님
        assertEquals(1, total)     // 전역 중복 없이 1건
        assertEquals(1, storage.loadRecords("2025").size)   // 올바른 연도 파일로 이동
        assertEquals(0, storage.loadRecords("2026").size)   // 잘못된 위치에서는 제거
        assertEquals("수정본", storage.loadRecordByYearAND("2025ZZZZ", "2025")?.courseName)

        scoreFile("2025").delete()
        scoreFile("2026").delete()
    }

    @Test
    fun records_import_collapsesExistingDuplicates() {
        storage.saveRecords(
            "2025",
            listOf(
                ScoreRecord(gameId = "2025DUP", courseName = "A"),
                ScoreRecord(gameId = "2025DUP", courseName = "B")
            )
        )

        val (added, total) = storage.importRecordsJson("[]")

        assertEquals(0, added)
        assertEquals(1, total)     // 기존 중복 1건으로 축소
        assertEquals(1, storage.loadRecords("2025").size)
        assertEquals("B", storage.loadRecordByYearAND("2025DUP", "2025")?.courseName)  // 마지막 값 유지

        scoreFile("2025").delete()
    }

    @Test
    fun records_importInvalidJson_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.importRecordsJson("[ { ")
        }
    }
}
