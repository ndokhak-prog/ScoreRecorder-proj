package com.example.scorerecorder.data

/**
 * 구장평가 기록 JSON (ndokhak_evaluation.json) - 구장당 1건(최신 유지)
 */
data class CourseEvaluation(
    val evalId: String = "",              // 평가Id (구장당 고정, 최초 저장 시 생성)
    val courseId: String = "",            // 구장정보Id
    val courseName: String = "",          // 구장명
    val gameDate: String = "",            // 경기일 yyyymmdd (평가 시점 표시)
    val scores: MutableList<Float?> = mutableListOf(), // 9개 항목 점수 (0~5.0, null=미입력)
    val remark: String = "",              // 특이사항 (100자 이내)
    val total: Float = 0f,               // 총평 (입력 항목 평균, 저장 시점 계산값)
    val updateDate: String = ""           // 입력일시 yyyy-MM-dd HH:mm:ss
)

/**
 * 구장평가 순수 계산 로직 (JVM 단위테스트 대상)
 */
object EvaluationEngine {

    const val ITEM_COUNT = 9
    const val MIN_SCORE = 0f
    const val MAX_SCORE = 5f

    /** 평가 항목명 (evaluation 화면 순서) */
    val ITEM_NAMES = listOf(
        "코스관리", "난이도", "코스구성", "시설", "예약편의",
        "접근성", "이용요금", "경관", "재방문"
    )

    /** 항목별 도움말 ( '?' 클릭 시 풍선도움말 ) */
    val ITEM_HELPS = listOf(
        "잔디 상태, 페어웨이, 그린 관리",
        "초급·중급·상급자에게 적절한지",
        "오르막·내리막, 벙커, 해저드, 전략성",
        "클럽하우스, 화장실, 휴게공간, 주차장",
        "예약 시스템, 대기시간",
        "내비 이동, 대중교통, 주차 편의",
        "가성비",
        "자연환경, 조망, 사진 촬영 포인트",
        "다시 방문하고 싶은 정도"
    )

    /**
     * 총평: 입력된 항목만 평균. 전체 미입력 시 null.
     */
    fun average(scores: List<Float?>): Float? {
        val entered = scores.filterNotNull()
        if (entered.isEmpty()) return null
        return entered.sum() / entered.size
    }

    /**
     * 별 표시용 0.5 단위 반올림.
     * 0.25점 이상 0.75점 미만 → 0.5 (별 반), 0.75점 이상 1.25점 미만 → 1.0 (별 하나) 규칙 포함.
     */
    fun starRound(v: Float): Float = Math.round(v * 2f) / 2f

    /**
     * 점수 정규화: 0~5.0 범위 클램프 + 0.5 단위 반올림.
     */
    fun snapScore(v: Float): Float =
        (Math.round(v.coerceIn(MIN_SCORE, MAX_SCORE) * 2f) / 2f).coerceIn(MIN_SCORE, MAX_SCORE)

    /**
     * 직접 입력 문자열 → 점수. 빈 값은 null, 잘못된 값(범위 밖/숫자 아님)도 null.
     */
    fun parseScoreInput(text: String): Float? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val v = t.toFloatOrNull() ?: return null
        if (v < MIN_SCORE || v > MAX_SCORE) return null
        return snapScore(v)
    }

    /**
     * 증감 버튼 값 → 표시 문자열 (예: 4 + 0.5 → "4.5")
     */
    fun stepText(text: String, delta: Float): String {
        val cur = parseScoreInput(text) ?: 0f
        return "%.1f".format(snapScore(cur + delta))
    }
}
