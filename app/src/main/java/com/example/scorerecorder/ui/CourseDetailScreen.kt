package com.example.scorerecorder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.data.*

/**
 * 구장 등록/수정 화면 (ndokhak_place.json)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    storage: Storage,
    courseId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onEvaluate: (courseId: String, courseName: String) -> Unit
) {
    val settings = remember { storage.loadSettings() }
    val editCourse = remember(courseId) {
        if (courseId.isNotEmpty()) storage.findPlaceById(courseId) else null
    }

    var region by remember { mutableStateOf(editCourse?.region ?: settings.region) }
    var courseName by remember { mutableStateOf(editCourse?.courseName ?: "") }
    val customTotal = editCourse?.totalHoles?.takeIf { it !in listOf(9, 18, 27, 36) }
    var holeValue by remember { mutableStateOf(if (editCourse != null && customTotal != null) -1 else editCourse?.totalHoles ?: settings.newHole) }
    var customHole by remember { mutableStateOf(customTotal?.toString() ?: "") }
    var location by remember { mutableStateOf(editCourse?.location ?: "") }
    var openTime by remember { mutableStateOf(editCourse?.openTime ?: "") }
    var closedDays by remember { mutableStateOf(editCourse?.closedDays ?: "") }
    var phone by remember { mutableStateOf(editCourse?.phone ?: "") }
    var extraInfo by remember { mutableStateOf(editCourse?.extraInfo ?: "") }
    val context = LocalContext.current

    // 주소 → 지도 앱 연결 (네이버지도 우선, 없으면 구글지도, 둘 다 없으면 비활성)
    val mapIntent = remember(location, context) {
        if (location.isBlank()) null else mapIntentFor(context, location)
    }

    val effHoles = if (holeValue in listOf(9, 18, 27, 36)) holeValue else customHole.toIntOrNull() ?: 0

    val distances = remember(editCourse, effHoles) {
        val base = editCourse?.courses?.flatMap { it.distances.dropLast(1) } ?: emptyList()
        mutableStateListOf(*Array(effHoles) { i -> base.getOrNull(i)?.takeIf { it > 0 } })
    }
    val pars = remember(editCourse, effHoles) {
        val base = editCourse?.courses?.flatMap { it.pars.dropLast(1) } ?: emptyList()
        mutableStateListOf(*Array(effHoles) { i -> base.getOrNull(i)?.takeIf { it > 0 } })
    }

    var showRegionDialog by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var parPick by remember { mutableStateOf<Int?>(null) }
    var dupConfirm by remember { mutableStateOf(false) }

    fun buildAndSave(force: Boolean = false): Boolean {
        val eff = if (holeValue in listOf(9, 18, 27, 36)) holeValue else customHole.toIntOrNull() ?: 0
        if (region.isEmpty()) { message = "지역을 선택하세요."; return false }
        if (courseName.isEmpty()) { message = "구장명을 입력하세요."; return false }
        if (courseName.length > 10) { message = "구장명은 10자 이내로 입력하세요."; return false }
        if (eff <= 0) { message = "전체홀을 입력하세요."; return false }

        // 신규 등록 시 동일한 구장명이 이미 저장되어 있으면 확인
        if (editCourse == null && !force && isDuplicateCourseName(storage.loadPlaces(), courseName)) {
            dupConfirm = true
            return false
        }

        val courses = List((eff + 8) / 9) { ci ->
            val start = ci * 9
            val idxs = (start until minOf(start + 9, eff)).toList()
            val dSum = idxs.sumOf { distances[it] ?: 0 }
            val pSum = idxs.sumOf { pars[it] ?: 0 }
            HoleCourse(
                code = "${('A'.code + ci).toChar()}",
                distances = idxs.map { distances[it] ?: 0 } + dSum,
                pars = idxs.map { pars[it] ?: 0 } + pSum
            )
        }
        val newCourse = GolfCourse(
            courseId = editCourse?.courseId ?: IdGen.unique(),
            region = region,
            courseName = CleanRules.cleanCourseName(courseName),
            totalHoles = eff,
            courses = courses,
            location = location,
            openTime = openTime,
            closedDays = closedDays,
            phone = phone.filter { it.isDigit() || it == '-' },
            extraInfo = extraInfo,
            updateDate = DateTimeUtils.now()
        )
        val list = storage.loadPlaces().toMutableList()
        val idx = list.indexOfFirst { it.courseId == newCourse.courseId }
        if (idx >= 0) list[idx] = newCourse else list.add(newCourse)
        storage.savePlaces(list)
        onSaved()
        return true
    }

    Scaffold(
        containerColor = ScaffoldCanvasColor,
        topBar = {
            AppTopBar(
                title = "구장 상세",
                onNavigate = { showCancelConfirm = true }
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("조회조건 ", fontSize = 13.sp)
                        Text(settings.courseQueryType, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Text("지역", fontSize = 13.sp)
                        OutlinedButton(onClick = { showRegionDialog = true }, modifier = Modifier.height(38.dp)) {
                            Text(region.ifEmpty { "선택" }, fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it.take(10) },
                        label = { Text("구장명 (10자 이내)") },
                        colors = inputFieldColors(),
                        textStyle = InputTextStyle,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("전체홀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    // 라디오 행간격 축소
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row {
                            listOf(9, 18, 27).forEach { h ->
                                RadioH(selected = holeValue == h, label = "${h}홀") { holeValue = h; customHole = "" }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioH(selected = holeValue == 36, label = "36홀") { holeValue = 36; customHole = "" }
                            Spacer(Modifier.width(4.dp))
                            OtherHoleField(
                                selected = holeValue !in listOf(9, 18, 27, 36),
                                value = customHole,
                                onValueChange = { customHole = it },
                                onSelect = { holeValue = -1 },
                                fieldHeight = 44.dp
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        List((effHoles + 8) / 9) { idx -> ('A'.code + idx).toChar() }.chunked(3).forEach { trio ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                trio.forEach { ch ->
                                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("${ch}코스(${courseColorLabel(ch - 'A')})", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 13.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (effHoles > 0) {
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val numC = (effHoles + 8) / 9
                        (0 until numC).map { idx -> ('A'.code + idx).toChar() }.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { ch ->
                                    CourseGrid(
                                        ch = ch,
                                        effHoles = effHoles,
                                        distances = distances,
                                        pars = pars,
                                        onParPick = { parPick = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 구장주소 (지도 버튼 추가로 좁아진 만큼 입력란 자동증가)
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(50) },
                    label = { Text("구장주소 (50자 이내)") },
                    colors = inputFieldColors(),
                    textStyle = InputTextStyle,
                    minLines = 1,
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { mapIntent?.let { context.startActivity(it) } },
                    enabled = mapIntent != null,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("지도", maxLines = 1)
                }
            }
            OutlinedTextField(value = openTime, onValueChange = { openTime = it.take(50) }, label = { Text("개장시간 (50자 이내)") }, colors = inputFieldColors(), textStyle = InputTextStyle, minLines = 1, maxLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = closedDays, onValueChange = { closedDays = it.take(50) }, label = { Text("휴장일 (50자 이내)") }, colors = inputFieldColors(), textStyle = InputTextStyle, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input -> phone = input.filter { it.isDigit() || it == '-' }.take(20) },
                    label = { Text("연락처") },
                    colors = inputFieldColors(),
                    textStyle = InputTextStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val digits = phone.filter { it.isDigit() }
                        if (digits.isNotEmpty()) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                        }
                    },
                    enabled = phone.any { it.isDigit() },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("전화", maxLines = 1)
                }
            }
            OutlinedTextField(
                value = extraInfo, onValueChange = { extraInfo = it.take(1000) }, label = { Text("기타정보 (1000자 이내)") },
                colors = inputFieldColors(),
                textStyle = InputTextStyle,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 12
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (editCourse != null) {
                    OutlinedButton(
                        onClick = { onEvaluate(editCourse!!.courseId, courseName) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) { Text("구장평가", maxLines = 1, fontSize = 12.sp) }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = { buildAndSave() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text("저장", maxLines = 1, fontSize = 12.sp) }
                TextButton(
                    onClick = { showCancelConfirm = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("취소", maxLines = 1, fontSize = 12.sp)
                }
            }
        }
        }
    }

    if (showRegionDialog) {
        ComboDialog("지역", Storage.REGIONS.split(","), region,
            onSelect = { region = it; showRegionDialog = false },
            onDismiss = { showRegionDialog = false })
    }
    parPick?.let { hi ->
        SingleValuePopup("파 (1~5)", pars[hi] ?: 0,
            onConfirm = { pars[hi] = it; parPick = null },
            onDismiss = { parPick = null },
            min = 1, max = 5)
    }
    message?.let { InfoMessage(it, onDismiss = { message = null }) }
    if (dupConfirm) {
        ConfirmMessage(
            text = "이미 등록되어 있는 구장명입니다.\n계속 등록하시겠습니까?",
            onYes = {
                dupConfirm = false
                buildAndSave(force = true)
            },
            onNo = { dupConfirm = false }
        )
    }
    if (showCancelConfirm) {
        ConfirmMessage(
            text = "저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = {
                showCancelConfirm = false
                buildAndSave()
            },
            onNo = {
                showCancelConfirm = false
                onBack()
            }
        )
    }
}

@Composable
private fun CourseGrid(
    ch: Char,
    effHoles: Int,
    distances: SnapshotStateList<Int?>,
    pars: SnapshotStateList<Int?>,
    onParPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val courseStart = (ch - 'A') * 9
    val idxs = (courseStart until minOf(courseStart + 9, effHoles)).toList()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${ch}코스(${courseColorLabel(ch - 'A')})", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            CellH("홀", 30.dp)
            CellH("거리(M)", 54.dp)
            CellH("파", 40.dp)
        }
        idxs.forEach { hi ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                    Text("${hi - courseStart + 1}", fontSize = 12.sp, maxLines = 1)
                }
                DistField(distances[hi], 54.dp) { d ->
                    distances[hi] = d
                    // 거리 변경 시 기준타 자동계산
                    pars[hi] = parForDistance(d)
                }
                Spacer(Modifier.width(4.dp))
                NumCellC(pars[hi], 40.dp) { onParPick(hi) }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            CellH("합계", 30.dp, bold = true)
            CellC(String.format(java.util.Locale.US, "%,d", idxs.sumOf { distances[it] ?: 0 }), 54.dp, bold = true)
            CellC("${idxs.sumOf { pars[it] ?: 0 }}", 40.dp, bold = true)
        }
    }
}

@Composable
private fun DistField(value: Int?, width: Dp, onValue: (Int?) -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .height(32.dp)
            .background(
                if (value == null) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        // 공백/0 은 초기값(-) 표시
        if (value == null) {
            Text("-", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BasicTextField(
            value = if (value == null) "" else String.format(java.util.Locale.US, "%,d", value),
            onValueChange = { t -> onValue(t.filter { it.isDigit() }.take(7).toIntOrNull()?.takeIf { it > 0 }) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun CellH(text: String, width: Dp, bold: Boolean = false) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CellC(text: String, width: Dp, bold: Boolean = false) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

/** 코스 색상 라벨 (적색, 청색, 황색, 흰색 순환) */
fun courseColorLabel(courseIdx: Int): String =
    listOf("적색", "청색", "황색", "흰색")[courseIdx % 4]

/** 거리(m) → 기준타 자동계산 (60 이하 3파, 110 미만 4파, 110 이상 5파) */
fun parFromDistance(d: Int): Int = when {
    d <= 60 -> 3
    d < 110 -> 4
    else -> 5
}

/** 거리 입력값 → 기준타 자동계산 (공백/null/0 → 0, 그 외 parFromDistance) */
fun parForDistance(d: Int?): Int = if (d == null || d <= 0) 0 else parFromDistance(d)

/**
 * 주소 문자열로 지도 앱 연결 Intent 생성.
 *  - 네이버지도 설치 시 네이버지도, 없으면 구글지도, 둘 다 없으면 null(비활성)
 */
fun mapIntentFor(context: Context, address: String): Intent? {
    val q = Uri.encode(address)
    val pm = context.packageManager
    val naver = "com.nhn.android.nmap"
    val google = "com.google.android.apps.maps"
    return when {
        pm.getLaunchIntentForPackage(naver) != null ->
            Intent(Intent.ACTION_VIEW, Uri.parse("nmap://search?query=$q&appname=${context.packageName}"))
        pm.getLaunchIntentForPackage(google) != null ->
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$q"))
                .setPackage(google)
        else -> null
    }
}

/** 이미 저장된 구장명과 중복인지 여부 (구장명은 정제 후 비교) */
fun isDuplicateCourseName(places: List<GolfCourse>, name: String, excludeId: String? = null): Boolean {
    val cleaned = CleanRules.cleanCourseName(name)
    return places.any { it.courseName == cleaned && (excludeId == null || it.courseId != excludeId) }
}

@Composable
private fun NumCellC(value: Int?, width: Dp, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(width).height(32.dp).clickableNoRipple { onClick() },
        color = if (value == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value?.toString() ?: "-", fontSize = 13.sp, maxLines = 1)
        }
    }
}