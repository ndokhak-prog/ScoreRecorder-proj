package com.example.scorerecorder.data

/**
 * 코스 정보 (A코스, B코스, C코스 ...) 9홀 단위
 */
data class HoleCourse(
    val code: String = "A",                 // A, B, C ...
    val distances: List<Int> = emptyList(), // 홀별 거리(m) 및 합계 포함(마지막)
    val pars: List<Int> = emptyList()       // 홀별 파(기준타) 및 합계 포함(마지막)
)

/**
 * 구장 정보 JSON (ndokhak_place.json)
 */
data class GolfCourse(
    val courseId: String = "",              // 구장정보Id (unique)
    val region: String = "",                // 지역
    val courseName: String = "",            // 구장명
    val totalHoles: Int = 0,                // 전체홀
    val courses: List<HoleCourse> = emptyList(),  // 코스 정보
    val location: String = "",              // 구장위치 (50자)
    val openTime: String = "",              // 개장시간 (50자)
    val closedDays: String = "",            // 휴장일 (50자)
    val phone: String = "",                 // 연락처 (숫자, - 포함)
    val extraInfo: String = "",             // 기타정보 (1000자)
    val updateDate: String = ""             // 입력일시 yyyy-MM-dd HH:mm:ss
)