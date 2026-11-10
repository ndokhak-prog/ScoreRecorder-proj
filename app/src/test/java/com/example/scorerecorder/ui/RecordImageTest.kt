package com.example.scorerecorder.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.scorerecorder.data.PlayerScore
import com.example.scorerecorder.data.ScoreRecord
import com.example.scorerecorder.testOutputDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 경기기록 이미지 렌더링/공유문구 테스트 (9차)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordImageTest {

    private fun sampleRecord() = ScoreRecord(
        gameId = "2026ABC123",
        courseName = "떼제베파크",
        gameDate = "20260922",
        region = "경기",
        totalHoles = 9,
        distances = listOf(480, 170, 330, 520, 400, 160, 350, 450, 210, 3070),
        pars = listOf(5, 3, 4, 5, 4, 3, 4, 4, 3, 35),
        scoreType = "A",
        players = mutableListOf(
            PlayerScore("홍길동", mutableListOf(5, 3, 4, 5, 4, 3, 4, 4, 3, 35), 35, 1, false),
            PlayerScore("김철수", mutableListOf(5, 4, 4, 5, 4, 3, 4, 4, 4, 37), 37, 2, false)
        ),
        updateDate = "2026-09-22 18:00:00"
    )

    @Test
    fun render_createsWideBitmap() {
        val bmp = RecordImage.render(sampleRecord())
        assertEquals(1080, bmp.width)
        assertTrue(bmp.height > 0)
    }

    @Test
    fun render_18HoleTwoCourse_noCrash() {
        val rec = sampleRecord().copy(totalHoles = 18)
        val bmp = RecordImage.render(rec)
        assertTrue(bmp.height > 0)
    }

    @Test
    fun render_courseSubtotalOnlyForMultiCourse() {
        // 9홀(단일 코스): 소계 행 없음 / 18홀(2코스): 홀 9행(rowH=40) + 코스 소계 2행(subH=44) 증가
        val h9 = RecordImage.render(sampleRecord().copy(totalHoles = 9)).height
        val h18 = RecordImage.render(sampleRecord().copy(totalHoles = 18)).height
        assertEquals(9 * 40 + 2 * 44, h18 - h9)
    }

    @Test
    fun holeMark_appliesStrokeRule() {
        assertEquals("-", holeMark(null, 4, "A"))
        assertEquals("-", holeMark(0, 4, "A"))
        assertEquals("P", holeMark(4, 4, "A"))
        assertEquals("B", holeMark(3, 4, "A"))
        assertEquals("+1", holeMark(5, 4, "A"))
        assertEquals("-", holeMark(null, 4, "R"))
        assertEquals("P", holeMark(0, 4, "R"))
        assertEquals("B", holeMark(-1, 4, "R"))
        assertEquals("+1", holeMark(1, 4, "R"))
    }

    @Test
    fun markColorArgb_distinctForEverySymbol() {
        val symbols = listOf("H", "A", "E", "B", "P", "+1", "+2", "+3", "+4", "2P")
        val colors = symbols.map { markColorArgb(it) }
        assertTrue("모든 심볼에 색상이 있어야 함", colors.all { it != null })
        assertEquals("심볼별 바탕색은 모두 달라야 함", symbols.size, colors.toSet().size)
        assertEquals(null, markColorArgb("5"))   // 심볼이 아닌 숫자는 색상 없음
        assertEquals(null, markColorArgb("-"))
    }

    @Test
    fun shareText_containsCoreInfo() {
        val t = RecordImage.shareText(sampleRecord())
        assertTrue(t.contains("떼제베파크"))
        assertTrue(t.contains("2026-09-22"))
        assertTrue(t.contains("홍길동"))
        assertTrue(t.contains("1위"))
        assertTrue(t.contains("총 35타"))
    }

    @Test
    fun encodeJpeg_under300kb() {
        val bytes = RecordImage.encodeJpeg(RecordImage.render(sampleRecord()))
        assertTrue(bytes.size > 0)
        assertTrue("jpeg size=${bytes.size}", bytes.size <= 300 * 1024)
    }

    @Test
    fun background_isQuarterSizeCenteredAndSaturated() {
        assertEquals(0.5f, RecordImage.BG_SCALE, 0.0001f)
        assertEquals(12f, RecordImage.BG_SATURATION, 0.0001f)
    }

    @Test
    fun render_withBackground_actuallyPaintsBackground() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val bg = RecordImage.loadBackground(ctx)
        assertTrue(bg != null)
        val rec = sampleRecord()
        val withBg = RecordImage.render(rec, bg)
        val noBg = RecordImage.render(rec, null)
        // 배경은 점수표 50% 크기로 중앙에 놓인다
        val regionW = withBg.width * RecordImage.BG_SCALE
        val regionH = regionW * bg!!.height / bg.width
        val left = (withBg.width - regionW) / 2f
        val top = (withBg.height - regionH) / 2f
        var diff = 0
        var y = top.toInt()
        val yEnd = (top + regionH).toInt()
        val xStart = left.toInt()
        val xEnd = (left + regionW).toInt()
        while (y < yEnd) {
            var x = xStart
            while (x < xEnd) {
                if (withBg.getPixel(x, y) != noBg.getPixel(x, y)) diff++
                x += 3
            }
            y += 3
        }
        assertTrue("background not painted in center, diff=$diff", diff > 500)
    }

    @Test
    fun render_writesSampleCardJpg() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val bg = RecordImage.loadBackground(ctx)
        val rec = sampleRecord().copy(totalHoles = 18, courseName = "제주회천")
        val bytes = RecordImage.encodeJpeg(RecordImage.render(rec, bg))
        val dir = testOutputDir()
        java.io.File(dir, "record_card_sample.jpg").writeBytes(bytes)
        assertTrue("jpeg size=${bytes.size}", bytes.size <= 300 * 1024)
    }

    @Test
    fun render_withBackground_under300kb() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val bg = RecordImage.loadBackground(ctx)
        assertTrue(bg != null)
        val bmp = RecordImage.render(sampleRecord().copy(totalHoles = 18), bg)
        val bytes = RecordImage.encodeJpeg(bmp)
        assertTrue("jpeg size=${bytes.size}", bytes.size <= 300 * 1024)
    }
}