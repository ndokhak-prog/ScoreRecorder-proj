package com.example.scorerecorder.data

/**
 * 설정 JSON (ndokhak_setting.json)
 */
data class GolfSettings(
    val settingId: String = "",                  // 설정 Id (unique)
    val nickname: String = "",                   // 주로 보여줄 선수 닉네임 (5자 이내)
    val nicknames: List<String> = emptyList(),   // 전체 선수 닉네임 목록 (닉네임 포함)
    val latestViewType: Int = 2,                 // 최근조회조건 1=일자별 2=선수별
    val latestPeriodMode: Int = 0,               // 최근기간 모드 0=기간 1=최근3개월
    val latestPeriodFr: String = "",             // 최근기간 fr yyyymm
    val latestPeriodTo: String = "",             // 최근기간 to yyyymm
    val latestPlayerName: String = "",           // 최근선수명
    val newHole: Int = 18,                       // 신규 홀 (9/18/27/36/기타)
    val scoreType: String = "A",                 // 점수입력방식 A=절대점수 R=상대점수
    val playerCount: Int = 1,                    // 선수인원수 1-4
    val courseQueryType: String = "구장명",       // 구장조회조건 (구장명/지역명)
    val region: String = "",                     // 지역
    val updateDate: String = ""                  // 입력일시 yyyy-MM-dd HH:mm:ss
)