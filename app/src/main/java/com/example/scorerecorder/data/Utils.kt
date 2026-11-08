package com.example.scorerecorder.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

object DateTimeUtils {

    private val displayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val storedFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun now(): String = dateTimeFmt.format(Date())

    fun todayStored(): String = storedFmt.format(Date())

    fun todayDisplay(): String = displayFmt.format(Date())

    /** yyyymmdd -> yyyy-mm-dd */
    fun toDisplay(date: String): String =
        if (date.length == 8) "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}" else date

    /** yyyymmdd -> mm-dd (연도 제외) */
    fun toDisplayMd(date: String): String =
        if (date.length == 8) "${date.substring(4, 6)}-${date.substring(6, 8)}" else date

    /** yyyy-mm-dd -> yyyymmdd */
    fun toStored(date: String): String = date.replace("-", "")

    fun yearOf(storedDate: String): String = storedDate.take(4)

    fun currentYearMonth(): String =
        SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())

    fun currentYear(): String =
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

    fun addMonths(ym: String, delta: Int): String {
        val year = ym.substring(0, 4).toInt()
        val month = ym.substring(4, 6).toInt()
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1 + delta, 1)
        return SimpleDateFormat("yyyyMM", Locale.getDefault()).format(cal.time)
    }

    fun isPastOrCurrent(ym: String): Boolean = ym <= currentYearMonth()

    /** 최근 3개월 시작 yyyymm (현재월 기준 2개월 전) */
    fun recent3MonthsFr(): String = addMonths(currentYearMonth(), -2)

    fun parseYmDisplay(ym: String): String =
        if (ym.length == 6) "${ym.substring(0, 4)}-${ym.substring(4, 6)}" else ym

    /**
     * 직접 입력한 년월 문자열을 yyyymm 으로 변환한다. (실패 시 null)
     * 허용: "2025-03", "2025.3", "202503", "20253"(=2025-03)
     */
    fun parseYmInput(input: String): String? {
        val digits = input.filter { it.isDigit() }
        val yearPart: String
        val monthPart: String
        when (digits.length) {
            6 -> { yearPart = digits.substring(0, 4); monthPart = digits.substring(4, 6) }
            5 -> { yearPart = digits.substring(0, 4); monthPart = "0" + digits.substring(4) }
            else -> return null
        }
        if (yearPart.toIntOrNull() == null) return null
        val month = monthPart.toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return yearPart + monthPart
    }

    /** yyyymmdd -> UTC 밀리sec (DatePicker용) */
    fun storedToMillis(date: String): Long? = try {
        java.time.LocalDate.parse(date, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (e: Exception) { null }

    /** UTC 밀리sec -> yyyymmdd (DatePicker용) */
    fun millisToStored(millis: Long): String = try {
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC)
            .toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
    } catch (e: Exception) { todayStored() }
}

object IdGen {
    fun unique(prefix: String = ""): String =
        prefix + UUID.randomUUID().toString().replace("-", "").uppercase(Locale.getDefault())

    /** 경기기록Id: 년도4자리 prefix + unique */
    fun gameId(year: String): String = unique(year)
}

/**
 * 저장 규칙 관련 문자열 정리
 */
object CleanRules {

    /** 개행/공백 제거 (닉네임, 선수명, 구장명) */
    fun removeWhitespace(s: String): String = s.replace("\\s".toRegex(), "")

    /** 프로 규칙: '프로' 포함 시 프로 단어 포함하여 이후 글자 삭제, 공백 제거 (구장명) */
    fun cleanCourseName(name: String): String {
        var n = name
        val idx = n.indexOf("프로")
        if (idx >= 0) n = n.substring(0, idx + "프로".length)
        return removeWhitespace(n)
    }

    /** 선수명: 5자 이내, '프로~' 포함 시 프로 포함 이후 삭제, 공백 제거 */
    fun cleanPlayerName(name: String): String {
        var n = name
        val idx = n.indexOf("프로")
        if (idx >= 0) n = n.substring(0, idx + "프로".length)
        n = removeWhitespace(n)
        return if (n.length > 5) n.substring(0, 5) else n
    }

    /** 파크 규칙: '파크' 포함 시 해당 단어 이후 삭제, 공백 제거 */
    fun cleanParkKeyword(name: String): String {
        var n = name
        val idx = n.indexOf("파크")
        if (idx >= 0) n = n.substring(0, idx + "파크".length)
        return removeWhitespace(n)
    }

    /** 코스 표시명 (프로님 추가) */
    fun displayNickname(nickname: String): String {
        if (nickname.isEmpty()) return "선수"
        return if (nickname.contains("프로")) nickname else "${nickname} 프로님"
    }

    /** 선수 표시명: 프로님 추가 (기록상세) */
    fun displayPlayer(name: String): String =
        if (name.contains("프로")) name else "${name} 프로"
}