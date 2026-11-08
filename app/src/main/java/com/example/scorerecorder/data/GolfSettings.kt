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

/**
 * 최근 조회기간(fr~to, yyyymm)을 설정값으로부터 계산한다.
 *  - latestPeriodMode = 1(최근3개월): 현재월 기준 3개월
 *  - 그 외: 저장된 기간(없으면 현재월)
 * 초기화면(MainScreen)과 기록 목록(RecordListScreen)이 같은 규칙을 사용한다.
 */
fun GolfSettings.latestPeriodRange(curYm: String = DateTimeUtils.currentYearMonth()): Pair<String, String> {
    return if (latestPeriodMode == 1) {
        DateTimeUtils.addMonths(curYm, -2) to curYm
    } else {
        latestPeriodFr.ifEmpty { curYm } to latestPeriodTo.ifEmpty { curYm }
    }
}