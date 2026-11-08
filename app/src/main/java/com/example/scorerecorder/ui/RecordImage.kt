package com.example.scorerecorder.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.scorerecorder.R
import com.example.scorerecorder.data.ScoreRecord
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 경기기록 상세를 이미지(Bitmap)로 렌더링하고
 * 갤러리 저장 / 카카오톡·문자 등 공유 기능을 제공한다.
 *  - 타이틀 한 줄 (N파크 스코어 경기결과_구장명)
 *  - 점수판 데이터 중앙정렬, 선수별 기록은 네이비 굵게
 *  - 배경이미지 적용, 생성 이미지 300KB 이하(JPEG)
 */
object RecordImage {

    private const val W = 1080
    private const val MAX_BYTES = 300 * 1024

    /** 배경이미지 크기 배율 (점수표 폭 대비, 중앙 배치) */
    const val BG_SCALE = 0.5f

    /** 배경이미지 채도 배수 (12배 진하게) */
    const val BG_SATURATION = 12f
    private val NAVY = Color.parseColor("#123A63")
    private val GOLD = Color.parseColor("#C9A24B")
    private val BLACK = Color.parseColor("#1C1C1A")
    private val GRAY = Color.parseColor("#5F5B52")
    private val LINE = Color.parseColor("#C9C4B8")
    private val HEAD_BG = Color.parseColor("#123A63")
    private val TOTAL_BG = Color.parseColor("#EFE7D4")
    private val SUB_BG = Color.parseColor("#F7F3EA")

    /** 배경 이미지 로드 (없으면 null) */
    fun loadBackground(context: Context): Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.score_bg)
    } catch (e: Exception) {
        null
    }

    /** 경기기록 → 경기결과 이미지 */
    fun render(rec: ScoreRecord, background: Bitmap? = null): Bitmap {
        val pCnt = max(rec.players.size, 1)
        val holeN = max(rec.totalHoles, 1)
        val pad = 28
        val colHole = 120
        val colDist = 120
        val colPar = 80
        val rem = W - pad * 2 - colHole - colDist - colPar
        val playerW = max(150, rem / pCnt)
        val rowH = 40
        val headH = 46
        val titleH = 54
        val infoH = 34
        val totH = 50
        val subH = 44
        val courseN = (holeN + 8) / 9
        // 단일 코스(9홀) 경기에서는 코스 소계 행을 표시하지 않는다(합계와 중복 방지)
        val subRows = if (courseN > 1) courseN else 0
        val rankH = 70 + pCnt * 34
        val height = pad * 2 + titleH + infoH + headH + holeN * rowH + subRows * subH + totH + rankH
        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // 바탕 (흰색) → 배경이미지(점수표 50% 크기, 중앙 배치, 12배 진하게)
        canvas.drawColor(Color.WHITE)
        if (background != null) {
            val bw = W * BG_SCALE
            val bh = bw * background.height / background.width
            val left = (W - bw) / 2f
            val top = (height - bh) / 2f
            val bgPaint = paint(0f, Typeface.DEFAULT, Color.WHITE).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(BG_SATURATION) })
            }
            canvas.drawBitmap(background, null, RectF(left, top, left + bw, top + bh), bgPaint)
        }

        val titleP = paint(34f, Typeface.DEFAULT_BOLD, NAVY).apply { textAlign = Paint.Align.CENTER }
        val infoP = paint(24f, Typeface.DEFAULT, GRAY).apply { textAlign = Paint.Align.CENTER }
        val headP = paint(28f, Typeface.DEFAULT_BOLD, Color.WHITE).apply { textAlign = Paint.Align.CENTER }
        val cellP = paint(28f, Typeface.DEFAULT, BLACK).apply { textAlign = Paint.Align.CENTER }
        val scoreP = paint(28f, Typeface.DEFAULT_BOLD, NAVY).apply { textAlign = Paint.Align.CENTER }
        val boldP = paint(28f, Typeface.DEFAULT_BOLD, NAVY).apply { textAlign = Paint.Align.CENTER }
        val smallP = paint(26f, Typeface.DEFAULT_BOLD, NAVY)
        val rankP = paint(26f, Typeface.DEFAULT_BOLD, NAVY)

        var yBase = pad.toFloat() + 36f
        // 타이틀 한 줄
        canvas.drawText("N파크 스코어 경기결과_${rec.courseName.ifEmpty { "구장" }}", W / 2f, yBase, titleP)

        yBase += 34f
        val info2 = buildString {
            append("경기일 ${dateDisplay(rec.gameDate)}")
            if (rec.region.isNotEmpty()) append("  ·  ${rec.region}")
            append("  ·  ${rec.totalHoles}홀")
            append("  ·  ${if (rec.scoreType == "A") "절대점수" else "상대점수"}")
        }
        canvas.drawText(info2, W / 2f, yBase, infoP)

        val xHole = pad.toFloat()
        val xDist = xHole + colHole
        val xPar = xDist + colDist
        val pXs = (0 until pCnt).map { xPar + colPar + it * playerW }
        val cHole = xHole + colHole / 2f
        val cDist = xDist + colDist / 2f
        val cPar = xPar + colPar / 2f
        val pCs = pXs.map { it + playerW / 2f }

        // 헤더행 (네이비 배경 + 흰색 중앙정렬)
        val headTop = yBase + 10f
        val headBottom = headTop + headH
        val totTop = headBottom + holeN * rowH.toFloat() + subRows * subH
        val totBot = totTop + totH
        // 점수판/순위 영역 흰 반투명 배경 (배경이미지 위 가독성 확보)
        val boardBg = solid(Color.argb(190, 255, 255, 255))
        canvas.drawRect(0f, headTop - 10f, W.toFloat(), totBot + 10f, boardBg)
        canvas.drawRect(0f, totBot + 16f, W.toFloat(), height - 6f, boardBg)
        canvas.drawRect(0f, headTop, W.toFloat(), headBottom, solid(HEAD_BG))
        val headBase = headBottom - 14f
        canvas.drawText("홀", cHole, headBase, headP)
        canvas.drawText("거리", cDist, headBase, headP)
        canvas.drawText("파", cPar, headBase, headP)
        rec.players.forEachIndexed { i, p ->
            canvas.drawText(p.name.ifEmpty { "선수${i + 1}" }, pCs[i], headBase, headP)
        }

        // 홀별 행 (데이터 중앙정렬) + 9홀 단위 코스별 소계
        val pars = rec.pars
        val dists = rec.distances
        val subP = paint(28f, Typeface.DEFAULT_BOLD, NAVY).apply { textAlign = Paint.Align.CENTER }
        var rowY = headBottom
        for (i in 0 until holeN) {
            val bot = rowY + rowH
            val base = bot - 12f
            canvas.drawText(holeLabel(i), cHole, base, cellP)
            canvas.drawText(dists.getOrNull(i)?.let { comma(it) } ?: "-", cDist, base, cellP)
            canvas.drawText(pars.getOrNull(i)?.toString() ?: "-", cPar, base, cellP)
            rec.players.forEachIndexed { j, p ->
                val mark = holeMark(p.scores.getOrNull(i), pars.getOrNull(i), rec.scoreType)
                drawMarkCell(canvas, pCs[j], rowY, rowH.toFloat(), mark, playerW.toFloat(), scoreP)
            }
            canvas.drawLine(0f, bot, W.toFloat(), bot, stroke(LINE))
            rowY = bot
            if (courseN > 1 && ((i + 1) % 9 == 0 || i == holeN - 1)) {
                val course = i / 9
                val start = course * 9
                val sBot = rowY + subH
                val sBase = sBot - 14f
                canvas.drawRect(0f, rowY, W.toFloat(), sBot, solid(SUB_BG))
                canvas.drawText("${('A'.code + course).toChar()}코스", cHole, sBase, subP)
                canvas.drawText(comma((start..i).sumOf { dists.getOrNull(it) ?: 0 }), cDist, sBase, subP)
                canvas.drawText((start..i).sumOf { pars.getOrNull(it) ?: 0 }.toString(), cPar, sBase, subP)
                rec.players.forEachIndexed { j, p ->
                    val s = (start..i).sumOf { p.scores.getOrNull(it) ?: 0 }
                    canvas.drawText(scoreDisplay(s, rec.scoreType), pCs[j], sBase, subP)
                }
                canvas.drawLine(0f, sBot, W.toFloat(), sBot, stroke(LINE))
                rowY = sBot
            }
        }

        // 합계 행
        canvas.drawRect(0f, totTop, W.toFloat(), totBot, solid(TOTAL_BG))
        val totBase = totBot - 14f
        canvas.drawText("합계", cHole, totBase, boldP)
        canvas.drawText(comma(dists.lastOrNull() ?: 0), cDist, totBase, boldP)
        canvas.drawText((pars.lastOrNull() ?: 0).toString(), cPar, totBase, boldP)
        rec.players.forEachIndexed { j, p ->
            canvas.drawText(p.total.toString(), pCs[j], totBase, boldP)
        }
        canvas.drawLine(0f, totBot, W.toFloat(), totBot, stroke(GOLD))

        // 세로 구분선 (중앙정렬 그리드)
        val vLine = stroke(LINE)
        canvas.drawLine(xDist, headTop, xDist, totBot, vLine)
        canvas.drawLine(xPar, headTop, xPar, totBot, vLine)
        for (i in 1 until pCnt) canvas.drawLine(pXs[i], headTop, pXs[i], totBot, vLine)

        // 선수 총타 및 순위 (좌측 정렬)
        var rankY = totBot + 34f
        canvas.drawText("선수 총타 및 순위", pad.toFloat(), rankY, smallP)
        rankY += 40f
        rec.players.sortedBy { it.rank }.forEach { p ->
            val rank = if (p.rank > 0) "${p.rank}위 " else ""
            val avg = if (holeN > 0) "${(p.total.toFloat() / holeN * 10).roundToInt() / 10.0}" else ""
            canvas.drawText("${rank}${p.name}  총 ${p.total}타  (홀당 평균 ${avg})", pad.toFloat(), rankY, rankP)
            rankY += 34f
        }

        return bmp
    }

    /** JPEG 인코딩 (300KB 이하가 되도록 품질 자동 조정) */
    fun encodeJpeg(bmp: Bitmap, maxBytes: Int = MAX_BYTES): ByteArray {
        val baos = ByteArrayOutputStream()
        var quality = 85
        while (true) {
            baos.reset()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (baos.size() <= maxBytes || quality <= 40) break
            quality -= 10
        }
        return baos.toByteArray()
    }

    /** 갤러리에 저장. 성공 시 안내 문구, 실패 시 오류 문구 반환 */
    fun saveToGallery(context: Context, rec: ScoreRecord): String {
        val bytes = encodeJpeg(render(rec, loadBackground(context)))
        val name = "ndokhak_${rec.gameId.take(14)}_${System.currentTimeMillis()}.jpg"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/N파크스코어")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return "이미지 저장에 실패했습니다."
                resolver.openOutputStream(uri).use { os ->
                    if (os == null) return "이미지 저장에 실패했습니다."
                    os.write(bytes)
                }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                "이미지가 '갤러리/N파크스코어' 폴더에 저장되었습니다."
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "N파크스코어").apply { mkdirs() }
                val f = File(dir, name)
                FileOutputStream(f).use { os -> os.write(bytes) }
                "이미지가 저장되었습니다.\n${f.absolutePath}"
            }
        } catch (e: Exception) {
            "이미지 저장 실패 : ${e.message}"
        }
    }

    /** 카카오톡·문자 등 공유 (이미지+문구, 실패 시 텍스트만) */
    fun share(context: Context, rec: ScoreRecord) {
        try {
            val bytes = encodeJpeg(render(rec, loadBackground(context)))
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val f = File(shareDir, "ndokhak_${rec.gameId.take(14)}.jpg")
            FileOutputStream(f).use { os -> os.write(bytes) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText(rec))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "경기기록 공유"))
        } catch (e: Exception) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText(rec))
            }
            context.startActivity(Intent.createChooser(send, "경기기록 공유"))
        }
    }

    /** 공유용 텍스트 요약 */
    fun shareText(rec: ScoreRecord): String = buildString {
        appendLine("[N파크 스코어] 경기결과")
        appendLine("구장명 : ${rec.courseName}")
        append("경기일 : ${dateDisplay(rec.gameDate)}")
        if (rec.region.isNotEmpty()) append("   지역 : ${rec.region}")
        appendLine()
        appendLine("전체홀 : ${rec.totalHoles}  점수방식 : ${if (rec.scoreType == "A") "절대점수" else "상대점수"}")
        appendLine()
        appendLine("선수 결과")
        rec.players.sortedBy { it.rank }.forEach { p ->
            val rank = if (p.rank > 0) "${p.rank}위 " else ""
            appendLine("${rank}${p.name} - 총 ${p.total}타")
        }
        appendLine()
        appendLine("저장일시 : ${rec.updateDate}")
    }

    /**
     * 홀별 선수 타수 셀: 변환값을 색상 네모상자에 표시 (경기기록화면 점수표시와 동일 기준)
     *  - 미입력/'-' : 상자 없이 '-'
     *  - 변환 심볼 : 심볼별 고유 배경색 상자 + 값
     *  - 그 외 숫자 : 옅은 회색 상자 + 값
     */
    private fun drawMarkCell(canvas: Canvas, cx: Float, top: Float, cellH: Float, mark: String, colW: Float, textP: Paint) {
        if (mark.isEmpty() || mark == "-") {
            textP.color = BLACK
            canvas.drawText(mark, cx, top + cellH - 12f, textP)
            return
        }
        val argb = markColorArgb(mark)
        val boxW = minOf(colW - 24f, 70f)
        val boxH = cellH - 8f
        val left = cx - boxW / 2f
        val bTop = top + 4f
        canvas.drawRoundRect(RectF(left, bTop, left + boxW, bTop + boxH), 8f, 8f, solid(argb ?: 0xFFECECEC.toInt()))
        textP.color = if (argb != null && isDarkColor(argb)) Color.WHITE else BLACK
        canvas.drawText(mark, cx, bTop + boxH - 8f, textP)
    }

    /** 밝기 기준 어두운 배경 여부 (글자 대비용) */
    private fun isDarkColor(argb: Int): Boolean {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) < 150
    }

    private fun paint(size: Float, tf: Typeface, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = tf
            this.color = color
        }

    private fun solid(color: Int): Paint = Paint().apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun stroke(color: Int): Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        this.color = color
    }

    private fun dateDisplay(d: String): String =
        if (d.length >= 8) "${d.take(4)}-${d.substring(4, 6)}-${d.substring(6, 8)}" else d

    /** 천단위 콤마 포맷 (예: 3070 -> "3,070") */
    private fun comma(n: Int): String = "%,d".format(n)

    private fun holeLabel(i: Int): String {
        val course = i / 9
        val no = i % 9 + 1
        return "${('A'.code + course).toChar()}-$no"
    }

    private fun scoreDisplay(v: Int?, scoreType: String): String {
        if (v == null) return "-"
        return if (scoreType == "R") {
            when {
                v > 0 -> "+$v"
                v < 0 -> "$v"
                else -> "0"
            }
        } else v.toString()
    }
}

/**
 * 이미지 홀별 선수 타수 표시 (타수 입력 변환규칙 적용)
 *  - 절대점수(A): 미입력(0)은 "-", 그 외 par 기준 strokeMark 심볼
 *  - 상대점수(R): par 대비 가감 후 strokeMark 심볼
 */
internal fun holeMark(v: Int?, par: Int?, scoreType: String): String {
    if (v == null || (scoreType != "R" && v == 0)) return "-"
    val strokes = if (scoreType == "R") (par ?: 0) + v else v
    return when {
        par != null && par > 0 -> strokeMark(strokes, par)
        scoreType == "R" && v > 0 -> "+$v"
        else -> "$v"
    }
}