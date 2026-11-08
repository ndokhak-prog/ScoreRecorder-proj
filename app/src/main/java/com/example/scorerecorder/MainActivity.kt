package com.example.scorerecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.scorerecorder.data.*
import com.example.scorerecorder.ui.*
import com.example.scorerecorder.ui.theme.ScoreRecorderTheme

class MainActivity : ComponentActivity() {

    private lateinit var storage: Storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(applicationContext)

        setContent {
            ScoreRecorderTheme {
                GolfApp(
                    storage = storage,
                    onFinished = { storage.saveSettings(storage.loadSettings()) }
                )
            }
        }
    }
}

/** 화면 종류 */
enum class ScreenKind { Main, Settings, NewGame, RecordDetail, CourseList, CourseDetail, RecordList, CourseEvaluation }

@Composable
fun GolfApp(storage: Storage, onFinished: () -> Unit) {

    var stack by remember { mutableStateOf(listOf(ScreenKind.Main)) }
    var bundle by remember { mutableStateOf<NavBundle>(NavBundle()) }
    var courseId by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<GolfCourse?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    fun push(screen: ScreenKind, b: NavBundle = NavBundle(), cid: String = "") {
        bundle = b
        courseId = cid
        stack = stack + screen
    }

    fun pop() {
        if (stack.size > 1) {
            val newStack = stack.dropLast(1)
            stack = newStack
        }
    }

    fun toMain() {
        stack = listOf(ScreenKind.Main)
        refreshKey++
    }

    // 구장선택 결과를 신규경기로 전달
    fun applyCourseAndReturn(course: GolfCourse?) {
        if (course != null) selectedPlace = course
        pop()
        refreshKey++
    }

    when (stack.last()) {
        ScreenKind.Main -> {
            MainScreen(
                storage = storage,
                onSettings = { push(ScreenKind.Settings, NavBundle(fromInitial = true)) },
                onNewGame = { push(ScreenKind.NewGame) },
                onRecordList = { push(ScreenKind.RecordList) },
                onCourseList = { push(ScreenKind.CourseList) },
                onRecordDetail = { id -> push(ScreenKind.RecordDetail, NavBundle(gameId = id)) }
            )
            if (refreshKey > 0) { /* Main 재조회는 자체 recompute */ }
        }

        ScreenKind.Settings -> {
            SettingsScreen(
                storage = storage,
                onBack = { pop() },
                onSaved = { toMain() }
            )
        }

        ScreenKind.NewGame -> {
            NewGameScreen(
                storage = storage,
                initial = bundle,
                selectedPlace = selectedPlace,
                onBack = { pop() },
                onGoCourseList = { push(ScreenKind.CourseList, NavBundle(fromInitial = false), "") },
                onSaved = { id, isNew ->
                    selectedPlace = null
                    push(ScreenKind.RecordDetail, NavBundle(gameId = id, isNew = isNew))
                }
            )
        }

        ScreenKind.RecordDetail -> {
            RecordDetailScreen(
                storage = storage,
                gameId = bundle.gameId,
                onBack = { selectedPlace = null; pop() },
                onRefresh = { refreshKey++ },
                onGoRecordList = { push(ScreenKind.RecordList) },
                onEvaluate = { cid, cname, date -> push(ScreenKind.CourseEvaluation, NavBundle(courseId = cid, courseName = cname, recordDate = date)) }
            )
        }

        ScreenKind.CourseList -> {
            CourseListScreen(
                storage = storage,
                selectForNewGame = stack.size >= 2 && stack[stack.size - 2] == ScreenKind.NewGame,
                onBack = { pop() },
                onAddCourse = { push(ScreenKind.CourseDetail, cid = "") },
                onEditCourse = { cid -> push(ScreenKind.CourseDetail, cid = cid) },
                onSelectCourse = { c -> applyCourseAndReturn(c) },
                onRefresh = { refreshKey++ },
                onEvaluate = { cid, cname -> push(ScreenKind.CourseEvaluation, NavBundle(courseId = cid, courseName = cname)) }
            )
        }

        ScreenKind.CourseDetail -> {
            CourseDetailScreen(
                storage = storage,
                courseId = courseId,
                onBack = { pop() },
                onSaved = { pop(); refreshKey++ },
                onEvaluate = { cid, cname -> push(ScreenKind.CourseEvaluation, NavBundle(courseId = cid, courseName = cname)) }
            )
        }

        ScreenKind.RecordList -> {
            RecordListScreen(
                storage = storage,
                onBack = { pop() },
                onOpen = { id -> push(ScreenKind.RecordDetail, NavBundle(gameId = id)) },
                onNewGame = { push(ScreenKind.NewGame) }
            )
        }

        ScreenKind.CourseEvaluation -> {
            CourseEvaluationScreen(
                storage = storage,
                initial = bundle,
                onBack = { pop() }
            )
        }
    }
}