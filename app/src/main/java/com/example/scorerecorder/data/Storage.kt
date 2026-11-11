package com.example.scorerecorder.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * JSON 파일 저장소
 *  - ndokhak_setting.json          : 설정
 *  - ndokhak_place.json            : 구장정보
 *  - ndokhak_score_yyyy.json       : 경기기록 (경기일 년도별)
 */
class Storage(private val context: Context) {

    private val gson = Gson()
    private val dir: File = context.filesDir

    private fun settingFile() = File(dir, SETTING_FILE)
    private fun placeFile() = File(dir, PLACE_FILE)
    private fun scoreFile(year: String) = File(dir, "ndokhak_score_$year.json")

    // ---------- 설정 ----------
    fun loadSettings(): GolfSettings {
        val f = settingFile()
        if (!f.exists()) {
            val def = defaultSettings()
            saveSettings(def)
            return def
        }
        return try {
            val type = object : TypeToken<GolfSettings>() {}.type
            val s = gson.fromJson<GolfSettings>(f.readText(), type)
            normalizeSettings(s)
        } catch (e: Exception) {
            val def = defaultSettings()
            saveSettings(def)
            def
        }
    }

    /** 구버전 설정(닉네임 1명)을 복수 목록으로 마이그레이션 */
    private fun normalizeSettings(s: GolfSettings?): GolfSettings {
        if (s == null) return defaultSettings()
        val names = (s.nicknames ?: emptyList()).filter { it.isNotBlank() }.map { it.trim().take(5) }.distinct()
        val nick = s.nickname?.trim()?.take(5) ?: ""
        val merged = when {
            names.isEmpty() && nick.isEmpty() -> emptyList()
            names.isEmpty() -> listOf(nick)
            nick.isNotEmpty() && nick !in names -> names + nick
            else -> names
        }
        val primary = if (nick.isNotEmpty()) nick else merged.firstOrNull() ?: ""
        return s.copy(nickname = primary, nicknames = merged)
    }

    fun saveSettings(s: GolfSettings) {
        settingFile().writeText(gson.toJson(s))
    }

    fun defaultSettings(): GolfSettings {
        val regionalList = REGIONS
        val currentYm = DateTimeUtils.currentYearMonth()
        val from = DateTimeUtils.addMonths(currentYm, -2)
        return GolfSettings(
            settingId = IdGen.unique(),
            nickname = "",
            latestViewType = 2,                     // 선수별
            latestPeriodFr = from,                  // 현재월-2
            latestPeriodTo = currentYm,             // 현재월
            latestPlayerName = "",
            newHole = 18,
            scoreType = "A",
            playerCount = 1,
            courseQueryType = "구장명",
            region = regionalList.split(",").first(),
            updateDate = DateTimeUtils.now()
        )
    }

    // ---------- 구장정보 ----------
    fun loadPlaces(): List<GolfCourse> {
        val f = placeFile()
        if (!f.exists()) { f.writeText("[]"); return emptyList() }
        return try {
            val type = object : TypeToken<List<GolfCourse>>() {}.type
            gson.fromJson(f.readText(), type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun savePlaces(list: List<GolfCourse>) {
        placeFile().writeText(gson.toJson(list))
    }

    fun findPlaceById(id: String): GolfCourse? = loadPlaces().find { it.courseId == id }

    fun findPlaceByName(name: String): GolfCourse? {
        val target = CleanRules.cleanCourseName(name)
        return loadPlaces().firstOrNull { CleanRules.cleanCourseName(it.courseName) == target }
    }

    // ---------- 경기기록 ----------
    fun loadRecords(year: String): List<ScoreRecord> {
        val f = scoreFile(year)
        if (!f.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ScoreRecord>>() {}.type
            gson.fromJson(f.readText(), type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveRecords(year: String, list: List<ScoreRecord>) {
        scoreFile(year).writeText(gson.toJson(list))
    }

    fun loadRecordById(id: String): Pair<String, ScoreRecord>? {
        val year = id.take(4)
        if (year.length != 4) return null
        return loadRecords(year).find { it.gameId == id }?.let { year to it }
    }

    fun loadRecordByYearAND(id: String, year: String): ScoreRecord? =
        loadRecords(year).find { it.gameId == id }

    /** 경기기록 삭제 (gameId의 년도파일에서) */
    fun deleteRecord(id: String): Boolean {
        val year = id.take(4)
        if (year.length != 4) return false
        val list = loadRecords(year)
        val remain = list.filterNot { it.gameId == id }
        if (remain.size == list.size) return false
        saveRecords(year, remain)
        return true
    }

    fun availableYears(): List<String> = dir.listFiles()
        ?.filter { it.name.startsWith("ndokhak_score_") && it.name.endsWith(".json") }
        ?.mapNotNull { it.name.removePrefix("ndokhak_score_").removeSuffix(".json") }
        ?.sortedDescending() ?: emptyList()

    /** 기간(fr~to, yyyymm)에 포함된 경기기록을 년도 파일들에서 추출 */
    fun loadRecordsInPeriod(fr: String, to: String): List<ScoreRecord> {
        val years = availableYears().ifEmpty { listOf(DateTimeUtils.currentYear()) }
        return years.flatMap { loadRecords(it) }
            .filter { rec ->
                val ym = rec.gameDate.take(6)
                ym >= fr && ym <= to
            }
            .sortedByDescending { it.gameDate }
    }

    /** 해당 기간 경기일(경기기록json)에 등록된 선수명 중복제거 */
    fun playerNamesInPeriod(fr: String, to: String): List<String> =
        loadRecordsInPeriod(fr, to)
            .flatMap { it.players.map { p -> p.name } }
            .distinct()
            .sorted()

    /** 가장 최근 경기 추출 */
    fun latestRecord(): ScoreRecord? =
        loadRecordsInPeriod("000000", "999999").firstOrNull()

    // ---------- 구장평가 ----------
    private fun evaluationFile() = File(dir, EVALUATION_FILE)

    fun loadEvaluations(): List<CourseEvaluation> {
        val f = evaluationFile()
        if (!f.exists()) { f.writeText("[]"); return emptyList() }
        return try {
            val type = object : TypeToken<List<CourseEvaluation>>() {}.type
            gson.fromJson(f.readText(), type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun saveEvaluations(list: List<CourseEvaluation>) {
        evaluationFile().writeText(gson.toJson(list))
    }

    /**
     * 구장 평가 조회.
     * - courseId가 있으면 courseId 기준, 없으면 구장명(정규화) 기준.
     * - 총평(입력 점수)이 있는 건만 최신으로 반환. (미입력 저장 건은 표시하지 않음)
     */
    fun findLatestEvaluation(courseId: String, courseName: String): CourseEvaluation? {
        val list = loadEvaluations().filter { ev -> ev.scores.any { it != null } }
        val byId = if (courseId.isNotBlank()) list.firstOrNull { it.courseId == courseId } else null
        if (byId != null) return byId
        val target = CleanRules.cleanCourseName(courseName)
        if (target.isBlank()) return null
        return list.filter { CleanRules.cleanCourseName(it.courseName) == target }
            .maxByOrNull { it.updateDate }
    }

    /** 구장 평가 저장 (courseId 기준 구장당 1건 갱신, 재평가 시 덮어쓰기) */
    fun saveEvaluation(ev: CourseEvaluation) {
        val list = loadEvaluations()
        val idx = list.indexOfFirst {
            if (ev.courseId.isNotBlank()) it.courseId == ev.courseId
            else CleanRules.cleanCourseName(it.courseName) == CleanRules.cleanCourseName(ev.courseName)
        }
        val newList = if (idx >= 0) list.toMutableList().apply {
            set(idx, ev.copy(evalId = this[idx].evalId.ifBlank { ev.evalId.ifBlank { IdGen.unique() } }))
        } else list + ev.copy(evalId = ev.evalId.ifBlank { IdGen.unique() })
        saveEvaluations(newList)
    }

    // ---------- 내보내기 / 가져오기 ----------
    /**
     * 구장정보 + 구장평가 전체를 JSON 문자열로 내보내기.
     * 형식: { "places": [...], "evaluations": [...] }
     */
    fun exportPlacesJson(): String {
        val root = JsonObject()
        root.add("places", gson.toJsonTree(loadPlaces()))
        root.add("evaluations", gson.toJsonTree(loadEvaluations()))
        return gson.toJson(root)
    }

    /** 경기기록 전체(모든 년도)를 JSON 문자열로 내보내기 */
    fun exportRecordsJson(): String {
        val all = availableYears().flatMap { loadRecords(it) }
        return gson.toJson(all)
    }

    /**
     * 구장정보 + 구장평가 병합 가져오기.
     *  - 구장: courseId 기준 (동일 courseId는 가져온 값으로 대체, 신규는 추가)
     *  - 평가: courseId(없으면 구장명) 기준 (동일 구장 평가는 가져온 값으로 대체)
     *  - 구버전 파일(구장정보 배열만 있는 JSON)도 그대로 가져올 수 있다.
     */
    fun importPlacesJson(json: String): PlaceImportResult {
        val (importedPlaces, importedEvals) = try {
            parsePlacesBundle(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("구장정보 파일 형식이 올바르지 않습니다.")
        }

        // --- 구장정보 병합 ---
        val map = LinkedHashMap<String, GolfCourse>()
        loadPlaces().forEach { if (it.courseId.isNotBlank()) map[it.courseId] = it }
        var added = 0
        importedPlaces.forEach { c ->
            if (c.courseId.isBlank()) return@forEach
            if (!map.containsKey(c.courseId)) added++
            map[c.courseId] = c
        }
        val merged = map.values.toList()
        savePlaces(merged)

        // --- 구장평가 병합 ---
        val evalMap = LinkedHashMap<String, CourseEvaluation>()
        loadEvaluations().forEach { ev -> evalKey(ev)?.let { evalMap[it] = ev } }
        var evalAdded = 0
        importedEvals.forEach { ev ->
            val key = evalKey(ev) ?: return@forEach
            val prev = evalMap[key]
            if (prev == null) evalAdded++
            // 같은 항목이면 기존 evalId 유지 (파일 값으로 나머지 대체)
            val id = prev?.evalId?.ifBlank { null } ?: ev.evalId.ifBlank { IdGen.unique() }
            evalMap[key] = ev.copy(evalId = id)
        }
        saveEvaluations(evalMap.values.toList())

        return PlaceImportResult(
            added = added,
            total = merged.size,
            evalAdded = evalAdded,
            evalTotal = evalMap.size
        )
    }

    /** 구장평가 병합 키 (구장 1건 원칙: courseId → 구장명 → evalId 순) */
    private fun evalKey(ev: CourseEvaluation): String? = when {
        ev.courseId.isNotBlank() -> "C:${ev.courseId}"
        CleanRules.cleanCourseName(ev.courseName).isNotBlank() ->
            "N:${CleanRules.cleanCourseName(ev.courseName)}"
        ev.evalId.isNotBlank() -> "E:${ev.evalId}"
        else -> null
    }

    /** 내보내기 파일 파싱: 신형({places,evaluations}) + 구형(구장정보 배열) 모두 지원 */
    private fun parsePlacesBundle(json: String): Pair<List<GolfCourse>, List<CourseEvaluation>> {
        val element = JsonParser.parseString(json)
        val placesType = object : TypeToken<List<GolfCourse>>() {}.type
        val evalType = object : TypeToken<List<CourseEvaluation>>() {}.type
        if (element.isJsonArray) {
            val places: List<GolfCourse> = gson.fromJson(element, placesType) ?: emptyList()
            return places to emptyList()
        }
        if (!element.isJsonObject) throw IllegalArgumentException("형식 오류")
        val obj = element.asJsonObject
        val places: List<GolfCourse> = obj.get("places")?.let { gson.fromJson(it, placesType) } ?: emptyList()
        val evals: List<CourseEvaluation> = obj.get("evaluations")?.let { gson.fromJson(it, evalType) } ?: emptyList()
        return places to evals
    }

    /**
     * 경기기록 병합 가져오기 (gameId 기준, 년도별 파일에 분배 저장).
     * 동일 gameId는 가져온 값으로 대체, 신규 gameId는 추가.
     * @return (신규 건수, 병합 후 전체 건수)
     */
    fun importRecordsJson(json: String): Pair<Int, Int> {
        val type = object : TypeToken<List<ScoreRecord>>() {}.type
        val imported: List<ScoreRecord> = try {
            gson.fromJson<List<ScoreRecord>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            throw IllegalArgumentException("경기기록 파일 형식이 올바르지 않습니다.")
        }
        // 기존 전체 기록을 gameId 기준으로 전역 정리 (연도 파일 간 중복 id도 1건으로 축소)
        val yearsBefore = availableYears()
        val merged = LinkedHashMap<String, ScoreRecord>()
        yearsBefore.forEach { year ->
            loadRecords(year).forEach { r -> if (r.gameId.isNotBlank()) merged[r.gameId] = r }
        }

        // 가져온 기록 병합: 동일 gameId는 파일 값으로 대체 → id 중복이 생기지 않음
        var added = 0
        imported.forEach { r ->
            if (r.gameId.length < 4) return@forEach
            if (!merged.containsKey(r.gameId)) added++
            merged[r.gameId] = r
        }

        // gameId의 연도 prefix 기준으로 재분배 저장 (기존 위치가 달라도 중복 생성 방지)
        val byYear = merged.values.groupBy { it.gameId.take(4) }
        (yearsBefore + byYear.keys).distinct().forEach { year ->
            saveRecords(year, byYear[year] ?: emptyList())
        }
        return added to merged.size
    }

    companion object {
        const val SETTING_FILE = "ndokhak_setting.json"
        const val PLACE_FILE = "ndokhak_place.json"
        const val EVALUATION_FILE = "ndokhak_evaluation.json"
        const val QUERY_TYPES = "구장명,지역명"
        const val REGIONS = "서울,경기,강원,충청,전라,경상,제주"
        const val HOLE_PRESETS = "9,18,27,36"
    }
}

/** 구장정보(+구장평가) 가져오기 결과 */
data class PlaceImportResult(
    val added: Int,
    val total: Int,
    val evalAdded: Int,
    val evalTotal: Int
)