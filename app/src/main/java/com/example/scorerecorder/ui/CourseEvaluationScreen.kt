package com.example.scorerecorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.widthIn
import com.example.scorerecorder.data.*

/**
 * 구장평가 화면 (ndokhak_evaluation.json)
 *  - 평가 항목 9개 (각 항목: 이름 + 도움말(?) + 별 표시 + 점수 입력(직접입력/증감버튼))
 *  - 점수: 0 ~ 5.0, 0.5 단위
 *  - 총 평: 입력된 항목만 평균 자동계산, 별은 0.5 단위 반올림
 *  - 저장: 구장당 1건 갱신 (courseId 기준, 재평가 시 덮어쓰기)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEvaluationScreen(
    storage: Storage,
    initial: NavBundle,
    onBack: () -> Unit
) {
    val courseId = initial.courseId
    val courseName = remember(courseId) {
        initial.courseName.ifEmpty { storage.findPlaceById(courseId)?.courseName ?: "" }
    }

    // 구장당 최신 평가 (총평/점수가 입력된 건만 표시)
    val saved = remember(courseId, courseName) { storage.findLatestEvaluation(courseId, courseName) }

    // 경기일: 기록상세에서 넘어온 경기일 > 해당 구장 최근 경기일 > 오늘
    val gameDate = remember(courseId, courseName, initial.recordDate) {
        initial.recordDate.ifEmpty {
            storage.loadRecordsInPeriod("000000", "999999")
                .firstOrNull { CleanRules.cleanCourseName(it.courseName) == CleanRules.cleanCourseName(courseName) }
                ?.gameDate ?: DateTimeUtils.todayStored()
        }
    }

    // 항목 점수 문자열 (직접입력 대응)
    val texts = remember(courseId, courseName) {
        mutableStateListOf(*Array(EvaluationEngine.ITEM_COUNT) { i ->
            saved?.scores?.getOrNull(i)?.let { "%.1f".format(it) } ?: ""
        })
    }
    var remark by remember(courseId, courseName) { mutableStateOf(saved?.remark ?: "") }

    // 점수 파싱 상태 (null=미입력)
    val parsed = remember(texts.toList()) { texts.map { EvaluationEngine.parseScoreInput(it) } }
    val avg = EvaluationEngine.average(parsed)

    var helpIndex by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageGoBack by remember { mutableStateOf(false) }
    var showBackConfirm by remember { mutableStateOf(false) }

    fun setScore(i: Int, v: Float) { texts[i] = "%.1f".format(v) }
    fun stepScore(i: Int, delta: Float) {
        val cur = EvaluationEngine.parseScoreInput(texts[i]) ?: 0f
        setScore(i, (cur + delta).coerceIn(EvaluationEngine.MIN_SCORE, EvaluationEngine.MAX_SCORE))
    }

    fun doSave() {
        val invalid = texts.indexOfFirst { it.isNotBlank() && EvaluationEngine.parseScoreInput(it) == null }
        if (invalid >= 0) {
            messageGoBack = false
            message = "${EvaluationEngine.ITEM_NAMES[invalid]} 점수는 0 ~ 5.0 사이 0.5 단위로 입력하세요."
            return
        }
        storage.saveEvaluation(
            CourseEvaluation(
                courseId = courseId,
                courseName = CleanRules.cleanCourseName(courseName),
                gameDate = gameDate,
                scores = parsed.toMutableList(),
                remark = remark.take(100),
                total = avg ?: 0f,
                updateDate = DateTimeUtils.now()
            )
        )
        messageGoBack = true
        message = "저장되었습니다."
    }

    Scaffold(
        containerColor = ScaffoldCanvasColor,
        topBar = {
            AppTopBar(
                title = "구장평가",
                onNavigate = { showBackConfirm = true }
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 제목 + 경기일
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${courseName.ifEmpty { "구장" }} 파크골프장 평가",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            "경기일 ${DateTimeUtils.toDisplay(gameDate).replace('-', '.')}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 평가 항목 9개
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        EvaluationEngine.ITEM_NAMES.forEachIndexed { i, name ->
                            EvalItemRow(
                                name = name,
                                help = EvaluationEngine.ITEM_HELPS[i],
                                text = texts[i],
                                onText = { texts[i] = it },
                                onMinus = { stepScore(i, -0.5f) },
                                onPlus = { stepScore(i, 0.5f) },
                                helpOpen = helpIndex == i,
                                onHelp = { helpIndex = if (helpIndex == i) null else i }
                            )
                            if (i < EvaluationEngine.ITEM_COUNT - 1) {
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }

                // 총 평 (입력된 항목만 평균, 별은 0.5 단위 반올림)
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("총 평", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(84.dp))
                        StarRow(score = avg?.let { EvaluationEngine.starRound(it) } ?: 0f)
                        Spacer(Modifier.weight(1f))
                        Text(
                            avg?.let { "%.1f".format(it) } ?: "-",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }

                // 특이사항 (100자 이내)
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("특이사항", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = remark,
                            onValueChange = { remark = it.take(100) },
                            placeholder = { Text("100자 이내") },
                            colors = inputFieldColors(),
                            textStyle = InputTextStyle,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Text(
                            "${remark.length} / 100",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text("닫기", maxLines = 1) }
                    Button(
                        onClick = { doSave() },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text("저장", maxLines = 1) }
                }
            }
        }
    }

    message?.let {
        InfoMessage(it, onDismiss = { val go = messageGoBack; message = null; if (go) onBack() })
    }
    if (showBackConfirm) {
        ConfirmMessage(
            text = "저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = { showBackConfirm = false; doSave() },
            onNo = { showBackConfirm = false; onBack() }
        )
    }
}

/** 평가 항목 1개 행: 이름 + ? + 별 + 점수입력(−/값/+) */
@Composable
private fun EvalItemRow(
    name: String,
    help: String,
    text: String,
    onText: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    helpOpen: Boolean,
    onHelp: () -> Unit
) {
    val value = EvaluationEngine.parseScoreInput(text)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(66.dp), maxLines = 1)
        // 도움말 아이콘 + 풍선도움말
        Box {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(18.dp).clickableNoRipple { onHelp() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            if (helpOpen) {
                Popup(
                    onDismissRequest = { onHelp() },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            help,
                            fontSize = 13.sp,
                            modifier = Modifier.widthIn(max = 240.dp).padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(3.dp))
        // 좁은 화면에서도 잘리지 않도록 남는 폭을 별 영역이 차지(최대 161dp)
        StarRow(score = value ?: 0f, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(3.dp))
        ScoreStepper(text = text, onText = onText, onMinus = onMinus, onPlus = onPlus)
    }
}

/** 점수 입력: − 버튼 + 숫자필드(직접입력) + + 버튼 (0.5 단위) */
@Composable
private fun ScoreStepper(
    text: String,
    onText: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val value = EvaluationEngine.parseScoreInput(text)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = onMinus,
            enabled = (value ?: 0f) > EvaluationEngine.MIN_SCORE,
            modifier = Modifier.width(28.dp).height(32.dp),
            contentPadding = PaddingValues(0.dp)
        ) { MinusGlyph() }
        CompactTextField(
            value = text,
            onValueChange = { input ->
                onText(input.filter { it.isDigit() || it == '.' }.take(4))
            },
            modifier = Modifier.width(56.dp),
            height = 32.dp,
            textAlign = TextAlign.Center,
            maxLength = 4,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        )
        OutlinedButton(
            onClick = onPlus,
            enabled = (value ?: 0f) < EvaluationEngine.MAX_SCORE,
            modifier = Modifier.width(28.dp).height(32.dp),
            contentPadding = PaddingValues(0.dp)
        ) { PlusGlyph() }
    }
}

/** − 기호: 폰트에 없는 문자여도 항상 보이도록 직접 그린다 */
@Composable
private fun MinusGlyph() {
    Box(Modifier.width(12.dp).height(2.dp).background(LocalContentColor.current))
}

/** + 기호: 폰트에 없는 문자여도 항상 보이도록 직접 그린다 */
@Composable
private fun PlusGlyph() {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.width(12.dp).height(2.dp).background(LocalContentColor.current))
        Box(Modifier.width(2.dp).height(12.dp).background(LocalContentColor.current))
    }
}

/** 별 표시 (0.5 단위 부분 별 지원): 앰버 채움 + 빈 별 회색 */
@Composable
fun StarRow(
    score: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 30.6f.dp,
    gap: Dp = 2.dp,
    filledColor: Color = Color(0xFFD97706),
    emptyColor: Color = Color(0xFFD9E2EC)
) {
    val fullWidth = starSize * 5 + gap * 4
    Canvas(modifier = modifier.defaultMinSize(minWidth = fullWidth).height(starSize)) {
        val g = gap.toPx()
        // 폭이 부족하면 별 크기를 줄여 잘리지 않게 한다
        val s = minOf(starSize.toPx(), (size.width - g * 4) / 5f)
        val startX = ((size.width - (s * 5 + g * 4)) / 2f).coerceAtLeast(0f)
        for (i in 0 until 5) {
            val left = startX + i * (s + g)
            val path = starPath(Offset(left + s / 2f, size.height / 2f), s / 2f)
            drawPath(path, color = emptyColor)
            val filled = (score - i).coerceIn(0f, 1f)
            if (filled > 0f) {
                clipRect(
                    left = left,
                    top = 0f,
                    right = left + s * filled,
                    bottom = size.height
                ) {
                    drawPath(path, color = filledColor)
                }
            }
        }
    }
}

/** 5각 별 Path (외곽 반지름 r 기준) */
private fun starPath(center: Offset, r: Float): Path {
    val path = Path()
    val inner = r * 0.45f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else inner
        val angle = Math.toRadians((-90 + i * 36).toDouble())
        val x = center.x + radius * kotlin.math.cos(angle).toFloat()
        val y = center.y + radius * kotlin.math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
