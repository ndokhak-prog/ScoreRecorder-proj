package com.example.scorerecorder.ui

import com.example.scorerecorder.data.GolfCourse
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 구장정보 목록 정렬(구장명 오름차순/내림차순) 검증
 */
class CourseSortTest {

    private fun course(name: String) = GolfCourse(courseId = name, courseName = name)

    @Test
    fun sortCoursesByName_ascending() {
        val input = listOf(course("다솜파크"), course("가을파크"), course("나무파크"))
        val names = sortCoursesByName(input, asc = true).map { it.courseName }
        assertEquals(listOf("가을파크", "나무파크", "다솜파크"), names)
    }

    @Test
    fun sortCoursesByName_descending() {
        val input = listOf(course("다솜파크"), course("가을파크"), course("나무파크"))
        val names = sortCoursesByName(input, asc = false).map { it.courseName }
        assertEquals(listOf("다솜파크", "나무파크", "가을파크"), names)
    }

    @Test
    fun sortCoursesByName_sameReturnForBothWhenSingle() {
        val input = listOf(course("한길파크"))
        assertEquals(1, sortCoursesByName(input, true).size)
        assertEquals(1, sortCoursesByName(input, false).size)
    }
}