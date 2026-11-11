package com.example.scorerecorder.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
    fun places_export_thenImport_restoresEvaluations() {
        storage.savePlaces(listOf(GolfCourse(courseId = "C1", courseName = "가")))
        storage.saveEvaluation(
            CourseEvaluation(
                courseId = "C1",
                courseName = "가",
                gameDate = "20261101",
                scores = mutableListOf(5f, 4f, null, null, null, null, null, null, null),
                remark = "잔디 좋음",
                total = 4.5f,
                updateDate = "2026-11-01 10:00:00"
            )
        )
        val exported = storage.exportPlacesJson()
        assertTrue("구장평가가 내보내기에 포함되어야 함", exported.contains("evaluations"))

        // 다른 기기(빈 저장소)로 가정하고 비운 뒤 가져오기
        storage.savePlaces(emptyList())
        storage.saveEvaluations(emptyList())

        val r = storage.importPlacesJson(exported)

        assertEquals(1, r.added)
        assertEquals(1, r.total)
        assertEquals(1, r.evalAdded)
        assertEquals(1, r.evalTotal)
        assertEquals("가", storage.loadPlaces().single().courseName)

        val ev = storage.loadEvaluations().single()
        assertEquals("C1", ev.courseId)
        assertEquals("잔디 좋음", ev.remark)
        assertEquals("20261101", ev.gameDate)
        assertEquals(4.5f, ev.total, 0.001f)
        assertEquals(5f, ev.scores[0]!!, 0.001f)
        assertNull(ev.scores[2])
        assertTrue(ev.evalId.isNotBlank())
    }

    @Test
    fun places_import_mergesEvaluationsByCourseId() {
        storage.savePlaces(emptyList())
        storage.saveEvaluations(
            listOf(
                CourseEvaluation(
                    evalId = "E1", courseId = "C1", courseName = "가",
                    remark = "이전", total = 3f, updateDate = "2026-01-01 00:00:00"
                )
            )
        )

        val json = """
            {"places":[{"courseId":"C1","courseName":"가"}],
             "evaluations":[{"evalId":"E9","courseId":"C1","courseName":"가",
                             "remark":"새값","total":4.2,"updateDate":"2026-02-02 00:00:00"}]}
        """.trimIndent()

        val r = storage.importPlacesJson(json)

        assertEquals(0, r.evalAdded)     // 같은 구장 → 신규 아님(대체)
        assertEquals(1, r.evalTotal)
        val ev = storage.loadEvaluations().single()
        assertEquals("새값", ev.remark)
        assertEquals(4.2f, ev.total, 0.001f)
        assertEquals("E1", ev.evalId)    // 기존 평가 id 유지
    }

    @Test
    fun places_import_oldArrayFormat_stillWorks() {
        storage.savePlaces(emptyList())
        storage.saveEvaluations(emptyList())

        val r = storage.importPlacesJson("""[{"courseId":"C9","courseName":"옛형식"}]""")

        assertEquals(1, r.added)
        assertEquals(1, r.total)
        assertEquals(0, r.evalTotal)     // 구버전 파일에는 평가 없음
        assertEquals("옛형식", storage.loadPlaces().single().courseName)
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
