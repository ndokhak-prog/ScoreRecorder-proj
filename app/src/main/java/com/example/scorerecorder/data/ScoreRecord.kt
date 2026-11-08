package com.example.scorerecorder.data

/**
 * 선수별 경기 정보
 */
data class PlayerScore(
    val name: String = "",                  // 선수명 (5자 이내, 프로 포함 저장)
    val scores: MutableList<Int?> = mutableListOf(),  // 홀별 점수 (null=미입력, 마지막=합계)
    val total: Int = 0,                     // 총타수(합계)
    val rank: Int = 0,                      // 순위 (0=제외/순위 없음)
    val excluded: Boolean = false           // 누락홀이 있어 순위 제외 여부
)

/**
 * 경기기록 JSON (ndokhak_score_yyyy.json) - 경기일의 년도별 파일
 */
data class ScoreRecord(
    val gameId: String = "",                // 경기기록Id (년도4자리 prefix + unique)
    val courseName: String = "",            // 구장명
    val gameDate: String = "",              // 경기일 yyyymmdd (8자리)
    val region: String = "",                // 지역
    val totalHoles: Int = 0,                // 전체홀
    val distances: List<Int> = emptyList(), // 홀별 거리(m) 합계 포함(마지막)
    val pars: List<Int> = emptyList(),      // 홀별 파(기준타) 합계 포함(마지막)
    val scoreType: String = "A",            // 점수입력방식 A/R
    val players: MutableList<PlayerScore> = mutableListOf(),  // 선수정보
    val updateDate: String = ""             // 입력일시 yyyy-MM-dd HH:mm:ss
)