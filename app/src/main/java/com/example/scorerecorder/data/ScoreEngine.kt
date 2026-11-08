package com.example.scorerecorder.data

/**
 * 순수 계산 로직 (JVM 단위테스트 대상)
 */
object ScoreEngine {

    /** 코스 수: 전체홀을 9홀 단위로 올림 */
    fun courseCount(totalHoles: Int): Int = (totalHoles + 8) / 9

    /**
     * 선수 총타수
     *  - A(절대점수): 입력값 단순 합계
     *  - R(상대점수): 기준타(파) 합계 + 가감값 합계
     */
    fun playerTotal(scores: List<Int?>, baseParAll: Int, scoreType: String): Int {
        val filled = scores.filterNotNull().sum()
        return if (scoreType == "R") baseParAll + filled else filled
    }

    /**
     * 전체 선수 총타수
     */
    fun computeTotals(players: List<List<Int?>>, baseParAll: Int, scoreType: String): List<Int> =
        players.map { playerTotal(it, baseParAll, scoreType) }

    /**
     * 순위 계산.
     * 전체홀(requiredHoles)을 모두 입력한 선수만 순위 부여(오름차순), 미완 선수는 0.
     * 동타일 경우 동순위(컴페티션 방식, 예: 1,2,2,4)를 부여한다.
     * 반환: players 인덱스 순서와 동일한 순위 리스트
     */
    fun computeRanks(
        players: List<List<Int?>>,
        requiredHoles: Int,
        baseParAll: Int = 0,
        scoreType: String = "A"
    ): List<Int> {
        val filled = players.map { it.filterNotNull().size }
        val totals = computeTotals(players, baseParAll, scoreType)
        val completeIdx = players.indices.filter { filled[it] == requiredHoles }
        val ordered = completeIdx.sortedBy { totals[it] }
        val orderedRanks = competitionRanks(ordered.map { totals[it] })
        val rankByIndex = HashMap<Int, Int>()
        ordered.forEachIndexed { i, idx -> rankByIndex[idx] = orderedRanks[i] }
        return players.indices.map { rankByIndex[it] ?: 0 }
    }

    /**
     * 컴페티션(동순위) 방식 순위 부여. 오름차순으로 정렬된 값 목록에 대해
     * 동일 값은 같은 순위, 다음 순위는 건너뛴다. 예) [70,75,75,80] → [1,2,2,4]
     */
    fun competitionRanks(valuesInAscendingOrder: List<Int>): List<Int> {
        val ranks = IntArray(valuesInAscendingOrder.size)
        var prev: Int? = null
        var prevRank = 0
        valuesInAscendingOrder.forEachIndexed { i, v ->
            val r = if (prev != null && v == prev) prevRank else i + 1
            ranks[i] = r
            prev = v
            prevRank = r
        }
        return ranks.toList()
    }
}