package com.example.scorerecorder.ui

import com.example.scorerecorder.data.GolfCourse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 신규 구장 등록 시 구장명 중복 확인 로직 검증
 */
class CourseDuplicateTest {

    private fun course(id: String, name: String) = GolfCourse(courseId = id, courseName = name)

    @Test
    fun duplicate_detected_whenNameExists() {
        val places = listOf(course("id1", "제주회천"), course("id2", "떼제베파크"))
        assertTrue(isDuplicateCourseName(places, "제주회천"))
        assertTrue(isDuplicateCourseName(places, "떼제베파크"))
    }

    @Test
    fun notDuplicate_whenNameNew() {
        val places = listOf(course("id1", "제주회천"))
        assertFalse(isDuplicateCourseName(places, "한라파크"))
    }

    @Test
    fun editMode_excludesSelf() {
        val places = listOf(course("id1", "제주회천"), course("id2", "떼제베파크"))
        assertFalse(isDuplicateCourseName(places, "제주회천", excludeId = "id1"))
        assertTrue(isDuplicateCourseName(places, "떼제베파크", excludeId = "id1"))
    }

    @Test
    fun emptyPlaces_isNotDuplicate() {
        assertFalse(isDuplicateCourseName(emptyList(), "제주회천"))
    }
}