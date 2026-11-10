package com.example.scorerecorder.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.data.*

/**
 * 설정 화면 (ndokhak_setting.json)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    storage: Storage,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val initial = remember { storage.loadSettings() }

    // ---- 상태 ----
    val nickList = remember {
        mutableStateListOf<String>().apply {
            addAll(initial.nicknames.ifEmpty { if (initial.nickname.isNotEmpty()) listOf(initial.nickname) else emptyList() })
        }
    }
    var primaryNick by remember { mutableStateOf(initial.nickname) }
    var viewType by remember { mutableStateOf(initial.latestViewType) }
    var periodMode by remember { mutableStateOf(initial.latestPeriodMode) }
    var periodFr by remember { mutableStateOf(initial.latestPeriodFr) }
    var periodTo by remember { mutableStateOf(initial.latestPeriodTo) }
    var playerName by remember { mutableStateOf(initial.latestPlayerName) }
    var newHole by remember { mutableStateOf(initial.newHole) }
    var customHole by remember { mutableStateOf(if (initial.newHole in HOLE_PRESET) "" else initial.newHole.toString()) }
    var scoreType by remember { mutableStateOf(initial.scoreType) }
    var playerCount by remember { mutableStateOf(initial.playerCount.toString()) }
    var queryType by remember { mutableStateOf(initial.courseQueryType) }
    var region by remember { mutableStateOf(initial.region) }

    // 다이얼로그 상태
    var showQueryTypeDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var showFrPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmSave by remember { mutableStateOf(false) }

    // ---- 내보내기 / 가져오기 (SAF 파일 선택/저장) ----
    val context = LocalContext.current

    val exportPlacesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            message = if (writeJsonToUri(context, uri, storage.exportPlacesJson()))
                "구장정보 내보내기가 완료되었습니다." else "구장정보 내보내기에 실패했습니다."
        }
    }
    val exportRecordsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            message = if (writeJsonToUri(context, uri, storage.exportRecordsJson()))
                "경기기록 내보내기가 완료되었습니다." else "경기기록 내보내기에 실패했습니다."
        }
    }
    val importPlacesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = readJsonFromUri(context, uri)
            message = when {
                json == null -> "구장정보 파일을 읽을 수 없습니다."
                else -> try {
                    val (added, total) = storage.importPlacesJson(json)
                    "구장정보 가져오기 완료 (신규 ${added}건 / 전체 ${total}건)"
                } catch (e: Exception) {
                    "구장정보 가져오기 실패: ${e.message}"
                }
            }
        }
    }
    val importRecordsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = readJsonFromUri(context, uri)
            message = when {
                json == null -> "경기기록 파일을 읽을 수 없습니다."
                else -> try {
                    val (added, total) = storage.importRecordsJson(json)
                    "경기기록 가져오기 완료 (신규 ${added}건 / 전체 ${total}건)"
                } catch (e: Exception) {
                    "경기기록 가져오기 실패: ${e.message}"
                }
            }
        }
    }

    fun cleanedNicknames(): List<String> =
        nickList.map { CleanRules.removeWhitespace(it).take(5) }.filter { it.isNotEmpty() }.distinct()

    fun primaryCleaned(): String = CleanRules.removeWhitespace(primaryNick).take(5)

    /** 홀 선택 (프리셋 선택 시 기타 입력값 공백 처리) */
    fun selectHole(h: Int) {
        newHole = h
        if (h in HOLE_PRESET) customHole = ""
    }

    /** 저장 시 반영할 닉네임 목록 (주로 보여줄 선수가 없으면 목록 첫번째가 주로 선수) */
    fun savedNicknames(): List<String> {
        val cleaned = cleanedNicknames()
        val primary = primaryCleaned()
        return when {
            cleaned.isEmpty() -> emptyList()
            primary.isNotEmpty() && primary !in cleaned -> listOf(primary) + cleaned
            else -> cleaned
        }
    }

    fun isDirty(): Boolean =
        primaryCleaned() != initial.nickname ||
            savedNicknames() != initial.nicknames ||
            viewType != initial.latestViewType ||
            periodMode != initial.latestPeriodMode ||
            periodFr != initial.latestPeriodFr ||
            periodTo != initial.latestPeriodTo ||
            playerName != initial.latestPlayerName ||
            newHole != initial.newHole ||
            customHole != (if (initial.newHole in HOLE_PRESET) "" else initial.newHole.toString()) ||
            scoreType != initial.scoreType ||
            playerCount != initial.playerCount.toString() ||
            queryType != initial.courseQueryType ||
            region != initial.region

    fun buildSettings(): GolfSettings {
        val hole = if (newHole in HOLE_PRESET) newHole else (customHole.toIntOrNull() ?: 0)
        val names = savedNicknames()
        val chosenNick = if (names.isEmpty()) ""
            else if (primaryCleaned().isNotEmpty() && primaryCleaned() in names) primaryCleaned() else names.first()
        return GolfSettings(
            settingId = initial.settingId.ifEmpty { IdGen.unique() },
            nickname = chosenNick,
            nicknames = names,
            latestViewType = viewType,
            latestPeriodMode = periodMode,
            latestPeriodFr = periodFr,
            latestPeriodTo = periodTo,
            latestPlayerName = CleanRules.removeWhitespace(playerName),
            newHole = hole,
            scoreType = scoreType,
            playerCount = playerCount.toIntOrNull() ?: 1,
            courseQueryType = queryType,
            region = region,
            updateDate = DateTimeUtils.now()
        )
    }

    fun validateForSave(): String? {
        if (cleanedNicknames().any { it.length > 5 }) return "닉네임은 5자까지만 입력 가능합니다."
        val cnt = playerCount.toIntOrNull() ?: return "선수인원수를 입력하세요. (1~4)"
        if (cnt < 1 || cnt > 4) return "인원수는 4명까지만 가능합니다."
        if (viewType == 1 && periodMode == 0) {
            if (periodFr.isEmpty() || periodTo.isEmpty()) return "기간을 입력하세요."
            if (periodFr > periodTo) return "기간 시작이 종료보다 늦을 수 없습니다."
            if (!DateTimeUtils.isPastOrCurrent(periodFr) || !DateTimeUtils.isPastOrCurrent(periodTo))
                return "기간은 현재월을 초과할 수 없습니다."
        }
        if (newHole !in HOLE_PRESET && (customHole.toIntOrNull() ?: 0) <= 0)
            return "기타 홀 값을 입력하세요."
        return null
    }

    fun save() {
        val error = validateForSave()
        if (error != null) { message = error; return }
        storage.saveSettings(buildSettings())
        onSaved()
    }

    fun requestBack() {
        if (isDirty()) confirmSave = true else onBack()
    }

    Scaffold(
        containerColor = ScaffoldCanvasColor,
        topBar = {
            AppTopBar(
                title = "설정",
                onNavigate = { requestBack() }
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // 선수 닉네임 (복수 등록)
            Section("선수 닉네임") {
                Text(
                    "주로 보여줄 선수 1명을 '주' 선택하세요. 나머지는 신규경기 닉네임 콤보 목록으로 사용됩니다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                nickList.forEachIndexed { idx, n ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioH(
                            selected = n.isNotEmpty() && primaryNick == n,
                            label = "주",
                            onClick = { if (n.isNotBlank()) primaryNick = n.trim() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CompactTextField(
                            value = n,
                            onValueChange = {
                                nickList[idx] = it.take(5)
                                if (it.length > 5) message = "닉네임은 5자까지만 입력 가능합니다."
                            },
                            placeholder = "닉네임 (5자 이내)",
                            filter = { !it.isWhitespace() },
                            maxLength = 5,
                            height = 40.dp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (primaryNick == nickList[idx].trim()) primaryNick = ""
                            nickList.removeAt(idx)
                        }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                    }
                }
                OutlinedButton(onClick = { nickList.add("") }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text("+ 닉네임 추가")
                }
            }

            // 최근기록 조회조건
            Section("최근기록 조회조건") {
                Row {
                    RadioH(selected = viewType == 1, label = "일자별") { viewType = 1 }
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioH(selected = viewType == 2, label = "선수별") { viewType = 2 }
                }

                val daily = viewType == 1
                if (daily) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioH(selected = periodMode == 0, label = "기간") { periodMode = 0 }
                        Spacer(modifier = Modifier.width(20.dp))
                        RadioH(selected = periodMode == 1, label = "최근3개월") { periodMode = 1 }
                    }

                    if (periodMode == 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("기간", fontSize = 13.sp)
                            OutlinedButton(
                                onClick = { showFrPicker = true },
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(DateTimeUtils.parseYmDisplay(periodFr), fontSize = 12.sp, maxLines = 1)
                            }
                            Text("~", fontSize = 13.sp)
                            OutlinedButton(
                                onClick = { showToPicker = true },
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(DateTimeUtils.parseYmDisplay(periodTo), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    } else {
                        Text(
                            "최근 3개월 : ${DateTimeUtils.parseYmDisplay(DateTimeUtils.recent3MonthsFr())} ~ ${DateTimeUtils.parseYmDisplay(DateTimeUtils.currentYearMonth())}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showPlayerDialog = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("선수명 : ${playerName.ifEmpty { primaryCleaned().ifEmpty { "선수" } }}", fontSize = 13.sp, maxLines = 1)
                    }
                    Text("선수명 콤보 : 해당 기간 경기에 등록된 선수명 목록", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 신규 경기
            Section("신규 경기") {
                Text("홀", fontSize = 13.sp, fontWeight = FontWeight.Bold)
Row {
                        HOLE_PRESET.dropLast(1).forEach { h ->
                            RadioH(selected = newHole == h, label = "${h}홀") { selectHole(h) }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Row {
                        RadioH(selected = newHole == HOLE_PRESET.last(), label = "${HOLE_PRESET.last()}홀") { selectHole(HOLE_PRESET.last()) }
                        Spacer(modifier = Modifier.width(8.dp))
                        OtherHoleField(
                            selected = newHole !in HOLE_PRESET,
                            value = customHole,
                            onValueChange = { customHole = it },
                            onSelect = { newHole = -1 }
                        )
                    }

                Text("점수입력방식", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row {
                    RadioH(selected = scoreType == "A", label = "절대점수 (A)") { scoreType = "A" }
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioH(selected = scoreType == "R", label = "상대점수 (R)") { scoreType = "R" }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("선수인원수", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = playerCount,
                        onValueChange = {
                            val filtered = it.filter { ch -> ch.isDigit() }.take(1)
                            playerCount = filtered
                            if (it.toIntOrNull() ?: 0 > 4) message = "인원수는 4명까지만 가능합니다."
                        },
                        placeholder = { Text("1-4명까지 가능", fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 1) },
                        singleLine = true,
                        colors = inputFieldColors(),
                        textStyle = InputTextStyle.copy(textAlign = TextAlign.End),
                        modifier = Modifier.width(72.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Text("명", fontSize = 13.sp)
                }
            }

            // 구장 정보
            Section("구장 정보") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("조회조건 ", fontSize = 14.sp)
                    OutlinedButton(onClick = { showQueryTypeDialog = true }, modifier = Modifier.height(40.dp)) {
                        Text(queryType, fontSize = 13.sp)
                    }
                    if (queryType == "지역명") {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("지역 ", fontSize = 14.sp)
                        OutlinedButton(onClick = { showRegionDialog = true }, modifier = Modifier.height(40.dp)) {
                            Text(region, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 데이터 내보내기 / 가져오기
            Section("데이터 내보내기 / 가져오기") {
                DataIoRow(
                    label = "구장정보",
                    onExport = { exportPlacesLauncher.launch("구장정보_${DateTimeUtils.todayStored()}.json") },
                    onImport = { importPlacesLauncher.launch(JSON_MIME) }
                )
                DataIoRow(
                    label = "경기기록",
                    onExport = { exportRecordsLauncher.launch("경기기록_${DateTimeUtils.todayStored()}.json") },
                    onImport = { importRecordsLauncher.launch(JSON_MIME) }
                )
                Text(
                    "내보내기: 저장 위치를 선택하면 JSON 파일이 생성됩니다.\n가져오기: 기존 데이터와 병합됩니다(같은 항목은 파일 값으로 대체).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { save() }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("저장")
                }
                TextButton(onClick = { requestBack() }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("취소")
                }
            }
        }
        }
    }

    if (showQueryTypeDialog) {
        ComboDialog("구장조회조건", Storage.QUERY_TYPES.split(","), queryType,
            onSelect = { queryType = it; showQueryTypeDialog = false },
            onDismiss = { showQueryTypeDialog = false })
    }
    if (showRegionDialog) {
        ComboDialog("지역", Storage.REGIONS.split(","), region,
            onSelect = { region = it; showRegionDialog = false },
            onDismiss = { showRegionDialog = false })
    }
    if (showPlayerDialog) {
        val queryFr = if (periodMode == 1) DateTimeUtils.recent3MonthsFr() else periodFr
        val queryTo = if (periodMode == 1) DateTimeUtils.currentYearMonth() else periodTo
        val names = storage.playerNamesInPeriod(queryFr, queryTo)
        ComboDialog("선수명 (해당 기간 등록 선수)", names.ifEmpty { listOf(primaryCleaned().ifEmpty { "선수" }) }, playerName,
            onSelect = { playerName = it; showPlayerDialog = false },
            onDismiss = { showPlayerDialog = false })
    }
    if (showFrPicker) {
        YearMonthPicker(periodFr, onPick = { periodFr = it; showFrPicker = false }, onDismiss = { showFrPicker = false })
    }
    if (showToPicker) {
        YearMonthPicker(periodTo, onPick = { periodTo = it; showToPicker = false }, onDismiss = { showToPicker = false })
    }
    message?.let { InfoMessage(it, onDismiss = { message = null }) }
    if (confirmSave) {
        ConfirmMessage(
            text = "저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = {
                confirmSave = false
                save()
            },
            onNo = {
                confirmSave = false
                onBack()
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            content()
        }
    }
}

private val JSON_MIME = arrayOf("application/json", "text/plain", "*/*")

@Composable
private fun DataIoRow(label: String, onExport: () -> Unit, onImport: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onExport, modifier = Modifier.height(40.dp)) { Text("내보내기") }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onImport, modifier = Modifier.height(40.dp)) { Text("가져오기") }
    }
}

/** JSON 문자열을 SAF Uri에 저장 (성공 여부 반환) */
private fun writeJsonToUri(context: Context, uri: Uri, json: String): Boolean = try {
    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
        out.write(json.toByteArray(Charsets.UTF_8))
        out.flush()
    }
    true
} catch (e: Exception) {
    false
}

/** SAF Uri에서 JSON 문자열 읽기 (실패 시 null) */
private fun readJsonFromUri(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
} catch (e: Exception) {
    null
}

private val HOLE_PRESET = listOf(9, 18, 27, 36)

/** yyyymm 선택 다이얼로그 (현재월 초과 불가) */
@Composable
private fun YearMonthPicker(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var ym by remember { mutableStateOf(current.ifEmpty { DateTimeUtils.currentYearMonth() }) }
    val currentYm = DateTimeUtils.currentYearMonth()

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("기간 선택") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { ym = DateTimeUtils.addMonths(ym, -1) }, enabled = ym.take(4).toInt() > 2000) {
                        Text("◀")
                    }
                    Text(DateTimeUtils.parseYmDisplay(ym), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    TextButton(onClick = { if (ym < currentYm) ym = DateTimeUtils.addMonths(ym, 1) }, enabled = ym < currentYm) {
                        Text("▶")
                    }
                }
                Row {
                    Text(
                        if (ym >= currentYm) "현재 월 이후로는 선택할 수 없습니다." else " ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(ym) }, enabled = ym <= currentYm) { Text("선택") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}