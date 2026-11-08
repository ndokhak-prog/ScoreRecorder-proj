package com.example.scorerecorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.example.scorerecorder.data.CourseEvaluation
import com.example.scorerecorder.data.DateTimeUtils
import com.example.scorerecorder.data.GolfCourse
import com.example.scorerecorder.data.HoleCourse
import com.example.scorerecorder.data.NavBundle
import com.example.scorerecorder.data.PlayerScore
import com.example.scorerecorder.data.ScoreRecord
import com.example.scorerecorder.data.Storage
import com.example.scorerecorder.ui.CourseDetailScreen
import com.example.scorerecorder.ui.CourseListScreen
import com.example.scorerecorder.ui.MainScreen
import com.example.scorerecorder.ui.NewGameScreen
import com.example.scorerecorder.ui.RecordDetailScreen
import com.example.scorerecorder.ui.RecordListScreen
import com.example.scorerecorder.ui.SettingsScreen
import com.example.scorerecorder.ui.theme.ScoreRecorderTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

/**
 * Robolectric로 실제 Compose 화면을 JVM에서 렌더링해 PNG 스크린샷을 저장한다.
 * (에뮬레이터/가상화 불필요)  app/build/screenshots 폴더에 PNG 생성
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class ScreenshotTest {

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun snap(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        view.setContent(content)
        activity.setContentView(view)

        ShadowLooper.idleMainLooper()

        val dm = activity.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        view.measure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        fun writeTo(file: File) {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        }
        val target = File(outDir, "$name.png")
        try {
            writeTo(target)
        } catch (e: java.io.IOException) {
            // 이미지 뷰어 등 다른 프로세스가 파일을 잠근 경우 대체 파일명으로 저장
            val alt = File(outDir, "$name-${System.currentTimeMillis()}.png")
            writeTo(alt)
            System.err.println("WARN: $name.png is locked by another process, wrote ${alt.name}")
        }
    }

    private fun newStorage(): Storage {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return Storage(context)
    }

    // ---------- fixtures ----------

    private fun buildCourse(
        id: String = "C001",
        region: String = "경기",
        name: String = "떼제베파크 CC"
    ): GolfCourse {
        val aPars = listOf(4, 5, 3, 4, 5, 4, 3, 5, 4)
        val aDist = listOf(350, 420, 180, 380, 450, 360, 170, 410, 390)
        return GolfCourse(
            courseId = id,
            region = region,
            courseName = name,
            totalHoles = 18,
            courses = listOf(
                HoleCourse("A", aDist + aDist.sum(), aPars + aPars.sum()),
                HoleCourse("B", aDist + aDist.sum(), aPars + aPars.sum())
            ),
            location = "경기도 성남시 분당구",
            openTime = "06:00 ~ 18:00",
            extraInfo = "부킹 가능"
        )
    }

    private fun buildRecord(): ScoreRecord {
        val pars = List(18) { listOf(4, 5, 3, 4, 5, 4, 3, 5, 4)[it % 9] }
        val dist = List(18) { listOf(350, 420, 180, 380, 450, 360, 170, 410, 390)[it % 9] }
        val gameDate = DateTimeUtils.todayStored()
        val year = gameDate.take(4)
        val p1Total = pars.map { it + 1 }.sum()
        val p2Total = pars.map { it - 1 }.sum()
        val p3Offs = pars.take(10).map { it + 2 } + List(8) { null }
        val p3Total = p3Offs.filterNotNull().sum()

        val player1 = PlayerScore(
            name = "홍길동",
            scores = (pars.map { it + 1 } + p1Total).toMutableList<Int?>(),
            total = p1Total,
            rank = 1,
            excluded = false
        )
        val player2 = PlayerScore(
            name = "김철수",
            scores = (pars.map { it - 1 } + p2Total).toMutableList<Int?>(),
            total = p2Total,
            rank = 2,
            excluded = false
        )
        val player3 = PlayerScore(
            name = "도민호",
            scores = (p3Offs + p3Total).toMutableList<Int?>(),
            total = p3Total,
            rank = 0,
            excluded = true
        )
        return ScoreRecord(
            gameId = year + "0001",
            courseName = "떼제베파크 CC",
            gameDate = gameDate,
            region = "경기",
            totalHoles = 18,
            distances = dist + dist.sum(),
            pars = pars + pars.sum(),
            scoreType = "A",
            players = mutableListOf(player1, player2, player3),
            updateDate = DateTimeUtils.now()
        )
    }

    private fun seedBase(storage: Storage) {
        storage.saveSettings(
            storage.defaultSettings().copy(
                nickname = "홍길동",
                latestViewType = 2,
                latestPlayerName = "홍길동"
            )
        )
        storage.savePlaces(listOf(buildCourse()))
        val record = buildRecord()
        storage.saveRecords(record.gameDate.take(4), listOf(record))
    }

    /** 구장평가 총평 표시 검증용 샘플 평가 (입력 4개 평균 = 4.0) */
    private fun seedEvaluation(storage: Storage) {
        storage.saveEvaluation(
            CourseEvaluation(
                courseId = "C001",
                courseName = "떼제베파크 CC",
                gameDate = DateTimeUtils.todayStored(),
                scores = mutableListOf(4.5f, 4.0f, 4.0f, 3.5f, null, null, null, null, null),
                remark = "",
                total = 4.0f,
                updateDate = DateTimeUtils.now()
            )
        )
    }

    // ---------- screens ----------

    @Test
    fun main_empty() {
        val storage = newStorage()
        storage.saveSettings(storage.defaultSettings())
        snap("main_empty") {
            ScoreRecorderTheme {
                MainScreen(storage, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun main_daily_view() {
        val storage = newStorage()
        seedBase(storage)
        storage.saveSettings(storage.loadSettings().copy(latestViewType = 1))
        snap("main_daily_view") {
            ScoreRecorderTheme {
                MainScreen(storage, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun main_player_view() {
        val storage = newStorage()
        seedBase(storage)
        snap("main_player_view") {
            ScoreRecorderTheme {
                MainScreen(storage, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun settings() {
        val storage = newStorage()
        seedBase(storage)
        snap("settings") {
            ScoreRecorderTheme {
                SettingsScreen(storage, {}, {})
            }
        }
    }

    @Test
    fun new_game() {
        val storage = newStorage()
        seedBase(storage)
        snap("new_game") {
            ScoreRecorderTheme {
                NewGameScreen(
                    storage = storage,
                    initial = NavBundle(),
                    selectedPlace = buildCourse(),
                    onBack = {},
                    onGoCourseList = {},
                    onSaved = { _, _ -> }
                )
            }
        }
    }

    // 지역명 '서울' 2글자 표시 검증용
    @Test
    fun new_game_region_seoul() {
        val storage = newStorage()
        seedBase(storage)
        snap("new_game_region_seoul") {
            ScoreRecorderTheme {
                NewGameScreen(
                    storage = storage,
                    initial = NavBundle(),
                    selectedPlace = buildCourse(region = "서울", id = "C002"),
                    onBack = {},
                    onGoCourseList = {},
                    onSaved = { _, _ -> }
                )
            }
        }
    }

    @Test
    fun record_detail() {
        val storage = newStorage()
        seedBase(storage)
        snap("record_detail") {
            ScoreRecorderTheme {
                RecordDetailScreen(
                    storage = storage,
                    gameId = buildRecord().gameId,
                    onBack = {},
                    onRefresh = {},
                    onGoRecordList = {},
                    onEvaluate = { _, _, _ -> }
                )
            }
        }
    }

    // 하단 버튼(저장/구장평가/취소)까지 담는 검증용 (버튼명 잘림 확인)
    @Test
    @Config(qualifiers = "w411dp-h1600dp-420dpi")
    fun record_detail_full() {
        val storage = newStorage()
        seedBase(storage)
        snap("record_detail_full") {
            ScoreRecorderTheme {
                RecordDetailScreen(
                    storage = storage,
                    gameId = buildRecord().gameId,
                    onBack = {},
                    onRefresh = {},
                    onGoRecordList = {},
                    onEvaluate = { _, _, _ -> }
                )
            }
        }
    }

    @Test
    fun record_list() {
        val storage = newStorage()
        seedBase(storage)
        snap("record_list") {
            ScoreRecorderTheme {
                RecordListScreen(storage, {}, {}, {})
            }
        }
    }

    @Test
    fun course_list() {
        val storage = newStorage()
        seedBase(storage)
        seedEvaluation(storage)
        snap("course_list") {
            ScoreRecorderTheme {
                CourseListScreen(storage, true, {}, {}, {}, {}, {}, { _, _ -> })
            }
        }
    }

    @Test
    fun course_detail() {
        val storage = newStorage()
        seedBase(storage)
        snap("course_detail") {
            ScoreRecorderTheme {
                CourseDetailScreen(storage, "C001", {}, {}, { _, _ -> })
            }
        }
    }

    @Test
    fun course_evaluation() {
        val storage = newStorage()
        seedBase(storage)
        snap("course_evaluation") {
            ScoreRecorderTheme {
                com.example.scorerecorder.ui.CourseEvaluationScreen(
                    storage = storage,
                    initial = NavBundle(courseId = "C001", courseName = "떼제베파크 CC"),
                    onBack = {}
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h891dp-420dpi")
    fun course_evaluation_narrow() {
        val storage = newStorage()
        seedBase(storage)
        seedEvaluation(storage)
        snap("course_evaluation_narrow") {
            ScoreRecorderTheme {
                com.example.scorerecorder.ui.CourseEvaluationScreen(
                    storage = storage,
                    initial = NavBundle(courseId = "C001", courseName = "떼제베파크 CC"),
                    onBack = {}
                )
            }
        }
    }
}