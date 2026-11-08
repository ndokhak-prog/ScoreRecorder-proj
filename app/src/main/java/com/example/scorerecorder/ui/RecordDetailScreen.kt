package com.example.scorerecorder.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.data.*

/**
 * 기록상세 화면 (ndokhak_score_yyyy.json)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    storage: Storage,
    gameId: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onGoRecordList: () -> Unit,
    onEvaluate: (courseId: String, courseName: String, gameDate: String) -> Unit
) {
    val record = remember(gameId) { storage.loadRecordById(gameId)?.second }

    if (record == null) {
        Scaffold(topBar = { AppTopBar(title = "기록상세", onNavigate = onBack) }) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("경기기록을 찾을 수 없습니다.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("이전") }
                }
            }
        }
        return
    }

    val holes = record.totalHoles
    val numCourses = (holes + 8) / 9
    val courseLetters = remember(holes) { List(numCourses) { ('A'.code + it).toChar() } }

    // 구장정보 (거리/파 자동표시용)
    val place = remember(record.courseName) { storage.findPlaceByName(record.courseName) }

    // ---- 편집 상태 ----
    val distances = remember(record, place) {
        val base = if (record.distances.isNotEmpty()) record.distances
            else place?.courses?.flatMap { it.distances.dropLast(1) } ?: emptyList()
        mutableStateListOf(*Array(holes) { i -> base.getOrNull(i)?.takeIf { it > 0 } })
    }
    val pars = remember(record, place) {
        val base = if (record.pars.isNotEmpty()) record.pars
            else place?.courses?.flatMap { it.pars.dropLast(1) } ?: emptyList()
        mutableStateListOf(*Array(holes) { i -> base.getOrNull(i)?.takeIf { it > 0 } })
    }
    val players = remember(record) {
        // 절대점수(A) 모드에서 미입력 저장값(0)은 '-'로 표시되도록 null 처리 (자동 알바트로스 방지)
        val isR = record.scoreType == "R"
        record.players.map { p ->
            PlayerEdit(
                name = p.name,
                scores = mutableStateListOf(*Array(holes) { i ->
                    p.scores.getOrNull(i)?.takeIf { isR || it != 0 }
                })
            )
        }.toMutableStateList()
    }
    val courseChecked = remember(record) { mutableStateListOf(*Array(numCourses) { true }) }
    val courseOrder = remember(record) { courseLetters.toMutableStateList() }
    val scoreType = record.scoreType

    val baseParAll = pars.sumOf { it ?: 0 }

    var dirty by remember { mutableStateOf(false) }
    fun setDirty() { dirty = true }

    // 점수 popup 상태: (playerIdx, holeIdx)
    var popupTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var distPick by remember { mutableStateOf<Int?>(null) }
    var parPick by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showBackConfirm by remember { mutableStateOf(false) }
    var confirmSave by remember { mutableStateOf<Boolean?>(null) }
    var gameDate by remember { mutableStateOf(record.gameDate) }
    var nameEditIdx by remember { mutableStateOf<Int?>(null) }
    var showDatePick by remember { mutableStateOf(false) }

    // 코스 드래그로 순서 변경 (1행 4개, 다른 행까지 이동가능)
    var dragIdx by remember { mutableStateOf(-1) }
    var dragX by remember { mutableStateOf(0f) }
    var dropIdx by remember { mutableStateOf(-1) }
    val chipLeft = remember { mutableListOf<Float>() }
    val chipTop = remember { mutableListOf<Float>() }
    val chipMid = remember { mutableListOf<Float>() }
    val chipMidY = remember { mutableListOf<Float>() }

    // 코스 sum 계산 (표시된 코스 기준)
    fun courseHoleIndices(courseIdx: Int): List<Int> {
        val start = courseIdx * 9
        return (start until minOf(start + 9, holes)).toList()
    }

    fun playerTotal(p: PlayerEdit): Int {
        val filled = p.scores.filterNotNull().sum()
        return if (scoreType == "R") baseParAll + filled else filled
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "기록 상세",
                onNavigate = {
                    if (dirty) showBackConfirm = true else onBack()
                }
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 정보 (경기일/선수명 편집 가능)
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("구장명 : ${record.courseName}", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("경기일 : ", fontSize = 12.sp)
                        TextButton(onClick = { showDatePick = true }, modifier = Modifier.height(34.dp)) {
                            Text(DateTimeUtils.toDisplay(gameDate), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("지역 : ${record.region}", fontSize = 12.sp, maxLines = 1)
                    }
                    Text("전체홀 : ${record.totalHoles}홀    점수방식 : ${if (scoreType == "R") "상대점수(R)" else "절대점수(A)"}", fontSize = 12.sp)
                    // 선수 3명 이상이면 1행에 2명씩 표시 (4명일 때 마지막 선수 잘림 방지)
                    if (players.size >= 3) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("선수 : ", fontSize = 12.sp)
                                players.withIndex().take(2).forEach { (i, p) ->
                                    TextButton(onClick = { nameEditIdx = i }, modifier = Modifier.height(34.dp)) {
                                        Text(CleanRules.displayPlayer(p.name), fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                            players.withIndex().drop(2).chunked(2).forEach { chunk ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 44.dp)) {
                                    chunk.forEach { (i, p) ->
                                        TextButton(onClick = { nameEditIdx = i }, modifier = Modifier.height(34.dp)) {
                                            Text(CleanRules.displayPlayer(p.name), fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("선수 : ", fontSize = 12.sp)
                            players.forEachIndexed { i, p ->
                                TextButton(onClick = { nameEditIdx = i }, modifier = Modifier.height(34.dp)) {
                                    Text(CleanRules.displayPlayer(p.name), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 코스 표시/숨김 + 드래그 순서 변경 (1행 3개, 다른 행 이동 가능)
            Text("코스 : ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            courseOrder.chunked(3).forEach { rowLetters ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    rowLetters.forEach { letter ->
                        val ord = courseOrder.indexOf(letter)
                        val ci = courseLetters.indexOf(letter)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (courseChecked[ci]) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .onGloballyPositioned { c ->
                                    val l = c.positionInRoot().x
                                    val t = c.positionInRoot().y
                                    val w = c.size.width.toFloat()
                                    val h = c.size.height.toFloat()
                                    if (chipLeft.size <= ord) {
                                        chipLeft.add(l); chipTop.add(t); chipMid.add(l + w / 2); chipMidY.add(t + h / 2)
                                    } else {
                                        chipLeft[ord] = l; chipTop[ord] = t; chipMid[ord] = l + w / 2; chipMidY[ord] = t + h / 2
                                    }
                                }
                                .graphicsLayer {
                                    translationX = if (dragIdx == ord) dragX else 0f
                                    translationY = if (dragIdx == ord) -6f else 0f
                                }
                                .pointerInput(letter, ord) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { dragIdx = ord; dropIdx = ord },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragX = amount.x
                                            if (chipLeft.size > ord) {
                                                val ptrX = chipLeft[ord] + change.position.x
                                                val ptrY = chipTop[ord] + change.position.y
                                                dropIdx = courseOrder.indices.minByOrNull { j ->
                                                    val dx = ptrX - chipMid[j]
                                                    val dy = ptrY - chipMidY[j]
                                                    dx * dx + dy * dy
                                                } ?: ord
                                            }
                                        },
                                        onDragEnd = {
                                            val from = dragIdx
                                            if (from >= 0 && from != dropIdx) {
                                                val item = courseOrder[from]
                                                courseOrder.removeAt(from)
                                                courseOrder.add(dropIdx, item)
                                                setDirty()
                                            }
                                            dragIdx = -1; dragX = 0f; dropIdx = -1
                                        },
                                        onDragCancel = { dragIdx = -1; dragX = 0f; dropIdx = -1 }
                                    )
                                }
                                .clickableNoRipple {
                                    courseChecked[ci] = !courseChecked[ci]
                                    setDirty()
                                }
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                "${letter}코스(${courseColorLabel(ci)})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                maxLines = 1,
                                color = if (courseChecked[ci]) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("순서변경 : 코스명을 누른 상태로 이동  /  표시·숨김 : 코스명 클릭", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("합계 : 기준타 $baseParAll  ${if (scoreType == "R") "(상대 가감)" else ""}",
                        modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 그리드 (가로 스크롤)
            val hScroll = rememberScrollState()
            // 4명까지 한 화면에 보이도록 열 너비를 가용폭에 맞춰 자동 조정
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val holeW = 42.dp
            val distW = 46.dp
            val parW = 32.dp
            val perPlayer = ((maxWidth - holeW - distW - parW) / players.size.coerceAtLeast(1).toFloat())
                .coerceIn(40.dp, 62.dp)
            val colWidths = arrayOf(holeW, distW, parW) + Array(players.size) { perPlayer }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(hScroll)) {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GridHeader("홀", colWidths[0])
                        GridHeader("거리", colWidths[1])
                        GridHeader("파", colWidths[2])
                        players.forEachIndexed { i, p ->
                            GridHeader(CleanRules.displayPlayer(p.name), colWidths[3 + i])
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                    courseOrder.forEach { letter ->
                        val ci = courseLetters.indexOf(letter)
                        if (!courseChecked[ci]) return@forEach
                        val idxs = courseHoleIndices(ci)
                        idxs.forEach { hi ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                GridCell("${letter}-${hi % 9 + 1}", colWidths[0])
                                NumberGridCell(distances[hi], colWidths[1], onPick = { distPick = hi })
                                NumberGridCell(pars[hi], colWidths[2], onPick = { parPick = hi })
                                players.forEachIndexed { pi, p ->
                                    MarkValueCell(
                                        value = p.scores[hi],
                                        par = pars[hi],
                                        isR = scoreType == "R",
                                        width = colWidths[3 + pi],
                                        onClick = { popupTarget = pi to hi }
                                    )
                                }
                            }
                        }
                        // 코스계
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            GridHeader("${letter}코스계", colWidths[0], color = MaterialTheme.colorScheme.tertiary)
                            val ds = idxs.sumOf { distances[it] ?: 0 }
                            GridCell(withComma(ds), colWidths[1], bold = true)
                            val ps = idxs.sumOf { pars[it] ?: 0 }
                            GridCell("$ps", colWidths[2], bold = true)
                            players.forEachIndexed { pi, p ->
                                val filled = idxs.map { p.scores[it] }.filterNotNull()
                                val sum = if (scoreType == "R") ps + filled.sum() else filled.sum()
                                GridCell("$sum", colWidths[3 + pi], bold = true)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    // 합계
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GridHeader("합계", colWidths[0], color = MaterialTheme.colorScheme.error)
                        GridCell(withComma(distances.sumOf { it ?: 0 }), colWidths[1], bold = true)
                        GridCell("$baseParAll", colWidths[2], bold = true)
                        players.forEachIndexed { pi, p ->
                            val filledSum = p.scores.filterNotNull().sum()
                            val total = if (scoreType == "R") baseParAll + filledSum else filledSum
                            GridCell("$total", colWidths[3 + pi], bold = true)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    // 순위 (타수 입력 시 실시간 반영, 동타 동순위)
                    val liveRanks = ScoreEngine.computeRanks(players.map { it.scores }, holes, baseParAll, scoreType)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GridHeader("순위", colWidths[0], color = MaterialTheme.colorScheme.primary)
                        GridCell("", colWidths[1])
                        GridCell("", colWidths[2])
                        players.forEachIndexed { pi, _ ->
                            val r = liveRanks.getOrElse(pi) { 0 }
                            GridCell(if (r > 0) "$r" else "-", colWidths[3 + pi], bold = true, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        saveRecord(storage, record, distances, pars, players, scoreType, courseChecked, courseOrder, courseLetters, holes, gameDate)
                        onRefresh()
                        message = "저장되었습니다."
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text("저장", maxLines = 1) }
                OutlinedButton(
                    onClick = { onEvaluate(place?.courseId ?: "", record.courseName, gameDate) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text("구장평가", maxLines = 1) }
                OutlinedButton(
                    onClick = { if (dirty) confirmSave = true else onBack() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("취소", maxLines = 1)
                }
            }
        }
        }
    }

    // popup
    popupTarget?.let { (pi, hi) ->
        if (players.getOrNull(pi) != null) {
            ScorePopupScreen(
                scoreType = scoreType,
                initial = players[pi].scores[hi],
                par = pars[hi],
                onDismiss = { popupTarget = null },
                onSelect = { v ->
                    players[pi].scores[hi] = v
                    setDirty()
                    popupTarget = null
                },
                // 해당홀의 참가선수 전체 점수 초기화
                onResetHole = {
                    players.forEach { it.scores[hi] = null }
                    setDirty()
                    popupTarget = null
                },
                // 선택한 점수를 참가선수 전체에 동일하게 입력
                onCopyToAll = { v ->
                    if (v != null) {
                        players.forEach { it.scores[hi] = v }
                        setDirty()
                    }
                    popupTarget = null
                }
            )
        }
    }
    distPick?.let { hi ->
        SingleValuePopup("거리 (m)", distances[hi] ?: 0,
            onConfirm = { distances[hi] = it; setDirty(); distPick = null },
            onDismiss = { distPick = null })
    }
    parPick?.let { hi ->
        SingleValuePopup("파 (1~5)", pars[hi] ?: 0,
            onConfirm = { pars[hi] = it; setDirty(); parPick = null },
            onDismiss = { parPick = null },
            min = 1, max = 5)
    }

    message?.let { InfoMessage(it, onDismiss = { message = null; onGoRecordList() }) }

    if (showBackConfirm) {
        ConfirmMessage(
            "저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = {
                showBackConfirm = false
                saveRecord(storage, record, distances, pars, players, scoreType, courseChecked, courseOrder, courseLetters, holes, gameDate)
                onRefresh()
                onBack()
            },
            onNo = {
                showBackConfirm = false
                onBack()
            }
        )
    }
    confirmSave?.let {
        ConfirmMessage(
            "저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = {
                confirmSave = null
                saveRecord(storage, record, distances, pars, players, scoreType, courseChecked, courseOrder, courseLetters, holes, gameDate)
                onRefresh()
                onBack()
            },
            onNo = {
                confirmSave = null
                onBack()
            }
        )
    }
    if (showDatePick) {
        DatePickDialog(gameDate, onPick = { gameDate = it; setDirty() }, onDismiss = { showDatePick = false })
    }
    nameEditIdx?.let { i ->
        val p = players.getOrNull(i) ?: run { nameEditIdx = null; return@let }
        TextInputDialog("선수명 (5자 이내)", p.name,
            onConfirm = { v ->
                val n = CleanRules.cleanPlayerName(v).take(5)
                if (n.isNotEmpty()) { p.name = n; setDirty() }
                nameEditIdx = null
            },
            onDismiss = { nameEditIdx = null })
    }
}

/** 편집 상태로부터 갱신된 ScoreRecord 생성 (합계/순위 재계산) */
private fun buildUpdatedRecord(
    record: ScoreRecord,
    distances: List<Int?>,
    pars: List<Int?>,
    players: List<PlayerEdit>,
    scoreType: String,
    holes: Int,
    gameDate: String
): ScoreRecord {
    val dSum = distances.sumOf { it ?: 0 }
    val pSum = pars.sumOf { it ?: 0 }
    val distStored = distances.map { it ?: 0 }.toMutableList().apply { add(dSum) }
    val parStored = pars.map { it ?: 0 }.toMutableList().apply { add(pSum) }

    // 순위 계산 (전 홀 입력 완료자만, ScoreEngine)
    val totals = ScoreEngine.computeTotals(players.map { it.scores }, pSum, scoreType)
    val ranks = ScoreEngine.computeRanks(players.map { it.scores }, holes, pSum, scoreType)

    val playerStored = players.mapIndexed { i, p ->
        val total = totals[i]
        val isComplete = ranks[i] > 0
        PlayerScore(
            name = p.name,
            scores = p.scores.map { it ?: 0 }.toMutableList<Int?>().apply { add(total) },
            total = total,
            rank = ranks[i],
            excluded = !isComplete
        )
    }

    return record.copy(
        gameDate = gameDate,
        distances = distStored,
        pars = parStored,
        players = playerStored.toMutableList(),
        updateDate = DateTimeUtils.now()
    )
}

/** 저장 로직 */
private fun saveRecord(
    storage: Storage,
    record: ScoreRecord,
    distances: List<Int?>,
    pars: List<Int?>,
    players: List<PlayerEdit>,
    scoreType: String,
    courseChecked: List<Boolean>,
    courseOrder: List<Char>,
    courseLetters: List<Char>,
    holes: Int,
    gameDate: String
) {
    val pSum = pars.sumOf { it ?: 0 }
    val updated = buildUpdatedRecord(record, distances, pars, players, scoreType, holes, gameDate)

    // 구장 신규등록 (구장명 기준 조회)
    if (storage.findPlaceByName(updated.courseName) == null) {
        val numCourses = (holes + 8) / 9
        val courses = courseLetters.take(numCourses).map { ch ->
            val ci = courseLetters.indexOf(ch)
            val start = ci * 9
            val idxs = (start until minOf(start + 9, holes)).toList()
            val ds = idxs.map { distances[it] ?: 0 }
            val ps = idxs.map { pars[it] ?: 0 }
            HoleCourse(
                code = "$ch",
                distances = ds + ds.sum(),
                pars = ps + ps.sum()
            )
        }
        val place = GolfCourse(
            courseId = IdGen.unique(),
            region = updated.region,
            courseName = updated.courseName,
            totalHoles = holes,
            courses = courses,
            updateDate = DateTimeUtils.now()
        )
        storage.savePlaces(storage.loadPlaces() + place)
    }

    val year = DateTimeUtils.yearOf(updated.gameDate)
    val list = storage.loadRecords(year).toMutableList()
    val idx = list.indexOfFirst { it.gameId == updated.gameId }
    if (idx >= 0) list[idx] = updated else list.add(updated)
    storage.saveRecords(year, list)
}

/** 편집 중 선수 상태 */
class PlayerEdit(var name: String, val scores: SnapshotStateList<Int?>)

@Composable
private fun GridHeader(text: String, width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color? = null) {
    Surface(color = if (color != null) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(width)) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun GridCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false, color: androidx.compose.ui.graphics.Color? = null) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            minLines = 1
        )
    }
}

@Composable
private fun MarkValueCell(
    value: Int?,
    par: Int?,
    isR: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    if (value == null || (!isR && value == 0)) {
        Surface(
            modifier = Modifier.width(width).clickableNoRipple { onClick() },
            color = MaterialTheme.colorScheme.background
        ) {
            Text("-", modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp)
        }
        return
    }
    val strokes = if (isR) (par ?: 0) + value else value
    val mark = if (par != null && par > 0) strokeMark(strokes, par) else
        if (isR && value > 0) "+$value" else "$value"
    Surface(
        modifier = Modifier.width(width).clickableNoRipple { onClick() },
        color = if (mark.toIntOrNull() != null || (par == null || par <= 0))
            MaterialTheme.colorScheme.surfaceVariant else markColor(mark)
    ) {
        Text(
            mark,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 타수 변환 규칙 (입력타수 s, 기준타 par)
 * 1 -> H / -3->A / -2->E / -1->B / 0->P / +1->+1 / +2->+2
 * (par>=4) +3 -> +3 / (par>=5) +4 -> +4 / s==par*2 -> 2P
 */
/** 천단위 콤마 포맷 (예: 3070 -> "3,070") */
private fun withComma(n: Int): String = "%,d".format(n)

fun strokeMark(s: Int, par: Int): String {
    if (s == 1) return "H"
    if (s == par * 2) return "2P"
    val d = s - par
    return when (d) {
        -3 -> "A"; -2 -> "E"; -1 -> "B"; 0 -> "P"; 1 -> "+1"; 2 -> "+2"
        3 -> if (par >= 4) "+3" else "$s"
        4 -> if (par >= 5) "+4" else "$s"
        else -> "$s"
    }
}

/** 변환 심볼별 고유 배경색 (ARGB). 심볼이 아니면 null */
internal fun markColorArgb(mark: String): Int? = when (mark) {
    "H" -> 0xFFFFD54F.toInt()    // 골드
    "A" -> 0xFFBA68C8.toInt()    // 보라
    "E" -> 0xFF4FC3F7.toInt()    // 하늘
    "B" -> 0xFF81C784.toInt()    // 그린
    "P" -> 0xFFFFF59D.toInt()    // 옅은 노랑
    "+1" -> 0xFFFFCC80.toInt()   // 연오렌지
    "+2" -> 0xFFFFB74D.toInt()   // 오렌지
    "+3" -> 0xFFE57373.toInt()   // 연빨강
    "+4" -> 0xFFD32F2F.toInt()   // 빨강
    "2P" -> 0xFF90A4AE.toInt()   // 청회색
    else -> null
}

/** 변환 심볼별 고유 배경색 (Compose Color) */
@Composable
private fun markColor(mark: String): Color =
    markColorArgb(mark)?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun NumberGridCell(
    value: Int?,
    width: androidx.compose.ui.unit.Dp,
    onPick: () -> Unit,
    onValue: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.width(width).clickableNoRipple { onPick() },
        color = if (value == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            value?.let { withComma(it) } ?: "-",
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

/** 단일 숫자 입력 팝업 (거리/파용) */
@Composable
fun SingleValuePopup(
    title: String,
    initial: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    min: Int = 0,
    max: Int = Int.MAX_VALUE
) {
    var text by remember { mutableStateOf(initial.coerceIn(min, max).takeIf { it > 0 }?.toString() ?: "") }
    fun current(): Int = (text.filter { it.isDigit() }.toIntOrNull() ?: min).coerceIn(min, max)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { val v = (current() - 1).coerceAtLeast(min); text = if (v == 0) "" else "$v" },
                    enabled = current() > min
                ) { Text("-", fontSize = 16.sp) }
                CompactTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    filter = { it.isDigit() },
                    maxLength = 5,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
                OutlinedButton(
                    onClick = { val v = (current() + 1).coerceAtMost(max); text = "$v" },
                    enabled = current() < max
                ) { Text("+", fontSize = 16.sp) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onConfirm(current()) }) { Text("확인", fontSize = 16.sp) }
                TextButton(onClick = onDismiss) { Text("닫기", fontSize = 16.sp) }
            }
        }
    )
}