package com.example.scorerecorder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코스 코드(중복 허용) 및 코스 홀정보 변환 테스트 */
class CourseCodesTest {

    private fun place(codes: List<String>): GolfCourse = GolfCourse(
        courseId = "C1",
        courseName = "테스트구장",
        totalHoles = codes.size * 9,
        courses = codes.mapIndexed { i, code ->
            HoleCourse(
                code = code,
                distances = List(9) { i * 100 + it } + 900,
                pars = List(9) { 4 } + 36
            )
        }
    )

    @Test
    fun defaults_startFromA_andFitKeepsDuplicates() {
        assertEquals(listOf("A", "B", "C"), CourseCodes.defaults(3))
        assertEquals(listOf("B", "B"), CourseCodes.fit(listOf("B", "B"), 2))
        assertEquals(listOf("B", "B", "C"), CourseCodes.fit(listOf("B", "B"), 3))
        assertEquals(listOf("B"), CourseCodes.fit(listOf("B", "C"), 1))
        assertEquals(listOf("A", "B"), CourseCodes.fit(emptyList(), 2))
    }

    @Test
    fun buildCourses_keepsDuplicateCodesAndUsesPositionalHoles() {
        val codes = listOf("B", "B")
        val distances = List(18) { 100 + it }
        val pars = List(18) { 4 }
        val courses = CourseInfo.buildCourses(codes, 18, distances, pars)

        assertEquals(listOf("B", "B"), courses.map { it.code })
        assertEquals(distances.take(9) + distances.take(9).sum(), courses[0].distances)
        assertEquals(distances.drop(9) + distances.drop(9).sum(), courses[1].distances)
    }

    @Test
    fun flatten_loadsRepeatedCourseByOccurrence() {
        val p = place(listOf("A", "B"))
        val flat = CourseInfo.flatten(p, listOf("B", "B"))

        assertEquals(p.courses[1].distances.dropLast(1), flat.first.take(9))
        assertEquals(p.courses[1].distances.dropLast(1), flat.first.drop(9))
        assertEquals(p.courses[1].pars.dropLast(1), flat.second.take(9))
    }

    @Test
    fun applyToPlace_updatesMatchingCodeAndLeavesOthers() {
        val p = place(listOf("A", "B"))
        val distances = List<Int?>(18) { 200 }
        val pars = List<Int?>(18) { 5 }
        val out = CourseInfo.applyToPlace(p, listOf("B", "B"), 18, distances, pars)

        assertEquals(2, out.courses.size)
        assertEquals("A", out.courses[0].code)
        assertEquals(p.courses[0].distances, out.courses[0].distances)   // 미대상 코스 유지
        assertEquals(List(9) { 200 } + 1800, out.courses[1].distances)  // 중복 코드는 마지막 블록 반영
        assertEquals(List(9) { 5 } + 45, out.courses[1].pars)
        assertTrue(out.updateDate.isNotBlank())
    }

    @Test
    fun forRecord_fillsLegacyRecordWithSequentialCodes() {
        val rec = ScoreRecord(gameId = "20261101X", totalHoles = 18)
        assertEquals(listOf("A", "B"), CourseCodes.forRecord(rec))

        val dup = ScoreRecord(gameId = "20261101Y", totalHoles = 18, courseCodes = listOf("B", "B"))
        assertEquals(listOf("B", "B"), CourseCodes.forRecord(dup))
    }
}
