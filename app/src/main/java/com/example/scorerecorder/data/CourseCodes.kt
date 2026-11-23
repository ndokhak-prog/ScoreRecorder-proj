package com.example.scorerecorder.data

/**
 * 9홀 단위 코스 코드(A~Z) 공용 유틸.
 * - 같은 코드를 여러 블록에 중복 지정할 수 있다 (예: 18홀을 B, B 로 플레이).
 * - 코드는 문자열이라 과거 데이터(코드 없음)도 안전하게 처리한다.
 */
object CourseCodes {
    const val HOLES_PER_COURSE = 9
    val ALL: List<String> = ('A'..'Z').map { it.toString() }

    fun isValid(code: String): Boolean = code.length == 1 && code[0] in 'A'..'Z'

    /** 개수만큼 A, B, C … 순서대로 생성 */
    fun defaults(count: Int): List<String> =
        (0 until count.coerceAtLeast(0)).map { ('A'.code + it).toChar().toString() }

    /**
     * 홀 수에 맞춰 코드 목록 개수를 보정한다.
     * 부족하면 마지막 코드 다음 글자부터 이어서 채우고, 많으면 뒤에서 잘라낸다.
     */
    fun fit(codes: List<String>, count: Int): List<String> {
        val n = count.coerceAtLeast(0)
        val src = codes.filter(::isValid)
        if (src.size >= n) return src.take(n)
        val lastCode = src.lastOrNull()?.firstOrNull()?.code ?: ('A'.code - 1)
        return src + (1..(n - src.size)).map { ('A'.code + (lastCode - 'A'.code + it)).toChar().toString() }
    }

    /** 코드 색상 라벨용 인덱스 (A=0, B=1 …) */
    fun colorIndex(code: String): Int =
        (code.firstOrNull()?.code?.let { it - 'A'.code } ?: 0).coerceIn(0, 25)

    /** n번째 9홀 블록에 속한 전체 홀 인덱스 */
    fun blockIndices(block: Int, holes: Int): List<Int> {
        val start = block * HOLES_PER_COURSE
        return (start until minOf(start + HOLES_PER_COURSE, holes)).toList()
    }

    /** 기록에 저장된 코드 목록이 없거나 홀 수와 맞지 않으면 보정해서 돌려준다 */
    fun forRecord(record: ScoreRecord): List<String> {
        val count = (record.totalHoles + HOLES_PER_COURSE - 1) / HOLES_PER_COURSE
        return fit(record.courseCodes, count)
    }
}

/** 코스별 홀 정보(거리/기준타)를 기록·구장정보 사이에서 변환하는 유틸 */
object CourseInfo {

    /** 한 9홀 블록을 HoleCourse 로 만든다 (합계를 마지막에 포함) */
    fun blockCourse(
        block: Int,
        code: String,
        holes: Int,
        distances: List<Int?>,
        pars: List<Int?>
    ): HoleCourse {
        val idxs = CourseCodes.blockIndices(block, holes)
        val ds = idxs.map { distances.getOrNull(it) ?: 0 }
        val ps = idxs.map { pars.getOrNull(it) ?: 0 }
        return HoleCourse(code = code, distances = ds + ds.sum(), pars = ps + ps.sum())
    }

    /** 코드 순서대로 HoleCourse 목록 생성 */
    fun buildCourses(
        codes: List<String>,
        holes: Int,
        distances: List<Int?>,
        pars: List<Int?>
    ): List<HoleCourse> = codes.mapIndexed { i, code -> blockCourse(i, code, holes, distances, pars) }

    /** 구장정보에서 특정 코드의 n번째 등장 블록 홀 정보를 읽는다 (없으면 9개 null) */
    fun holesOf(
        place: GolfCourse?,
        code: String,
        occurrence: Int = 0
    ): Pair<List<Int?>, List<Int?>> {
        val matches = place?.courses?.filter { it.code == code }.orEmpty()
        val course = matches.getOrNull(occurrence) ?: matches.firstOrNull()
        if (course == null) return List(CourseCodes.HOLES_PER_COURSE) { null } to
                List(CourseCodes.HOLES_PER_COURSE) { null }
        val d = course.distances.dropLast(1)
        val p = course.pars.dropLast(1)
        return List(CourseCodes.HOLES_PER_COURSE) { i -> d.getOrNull(i)?.takeIf { it > 0 } } to
                List(CourseCodes.HOLES_PER_COURSE) { i -> p.getOrNull(i)?.takeIf { it > 0 } }
    }

    /** 코드 순서대로 구장정보의 홀 정보를 펼쳐 기록용 플랫 배열로 만든다 */
    fun flatten(
        place: GolfCourse?,
        codes: List<String>
    ): Pair<List<Int?>, List<Int?>> {
        val used = mutableMapOf<String, Int>()
        val distances = mutableListOf<Int?>()
        val pars = mutableListOf<Int?>()
        codes.forEach { code ->
            val occ = used[code] ?: 0
            used[code] = occ + 1
            val (d, p) = holesOf(place, code, occ)
            distances.addAll(d)
            pars.addAll(p)
        }
        return distances to pars
    }

    /**
     * 기록의 코스 순서대로 구장정보 코스 홀 정보를 현행화한다.
     * 같은 코드가 중복되면 마지막 블록 값이 적용된다.
     */
    fun applyToPlace(
        place: GolfCourse,
        codes: List<String>,
        holes: Int,
        distances: List<Int?>,
        pars: List<Int?>
    ): GolfCourse {
        val courses = place.courses.toMutableList()
        codes.forEachIndexed { i, code ->
            val updated = blockCourse(i, code, holes, distances, pars)
            val idx = courses.indexOfFirst { it.code == code }
            if (idx >= 0) courses[idx] = updated else courses.add(updated)
        }
        return place.copy(courses = courses, updateDate = DateTimeUtils.now())
    }
}
