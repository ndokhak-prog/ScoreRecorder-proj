package com.example.scorerecorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.data.*

/**
 * 신규경기 화면 (ndokhak_score_yyyy.json 저장)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGameScreen(
    storage: Storage,
    initial: NavBundle,
    selectedPlace: GolfCourse?,
    onBack: () -> Unit,
    onGoCourseList: () -> Unit,
    onSaved: (gameId: String, isNew: Boolean) -> Unit
) {
    val settings = remember { storage.loadSettings() }

    var courseId by remember(selectedPlace, initial) { mutableStateOf(
        if (selectedPlace != null) selectedPlace.courseId else initial.courseId
    ) }
    var courseName by remember(selectedPlace, initial) { mutableStateOf(
        if (selectedPlace != null) selectedPlace.courseName else initial.courseName
    ) }
    var gameDate by remember { mutableStateOf(initial.recordDate.ifEmpty { DateTimeUtils.todayStored() }) }
    var region by remember(selectedPlace, initial) { mutableStateOf(
        selectedPlace?.region?.ifEmpty { settings.region } ?: settings.region
    ) }
    var holeValue by remember(selectedPlace, initial) { mutableStateOf(
        if (selectedPlace != null) selectedPlace.totalHoles
        else if (initial.totalHoles > 0) initial.totalHoles
        else settings.newHole
    ) }
    var customHole by remember(selectedPlace) { mutableStateOf(
        selectedPlace?.totalHoles?.takeIf { it !in listOf(9, 18, 27, 36) }?.toString() ?: ""
    ) }
    var scoreType by remember { mutableStateOf(settings.scoreType) }
    var players by remember {
        mutableStateOf(
            if (initial.playerNames.isNotEmpty())
                initial.playerNames.take(4).map { it }.toMutableStateList()
            else
                (0 until settings.playerCount.coerceIn(1, 4)).map { "" }.toMutableStateList()
        )
    }

    var showRegionDialog by remember { mutableStateOf(false) }
    var showDatePick by remember { mutableStateOf(false) }
    var showNickDialog by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showBackConfirm by remember { mutableStateOf(false) }

    // 닉네임 콤보 목록 : 주로 보여줄 선수 맨 위 + 나머지 가나다순
    val nickNames = remember(settings) {
        val all = (settings.nicknames.ifEmpty { if (settings.nickname.isNotEmpty()) listOf(settings.nickname) else emptyList() })
            .filter { it.isNotEmpty() }
        all.filter { it == settings.nickname } + all.filter { it != settings.nickname }.sorted()
    }

    val numCourses = (if (holeValue in listOf(9, 18, 27, 36)) holeValue else customHole.toIntOrNull() ?: 0 + 8) / 9

    fun backOrConfirm() {
        val dirty = courseId.isNotEmpty() || courseName.isNotEmpty() || holeValue != settings.newHole ||
            players.any { it.isNotEmpty() }
        if (dirty) showBackConfirm = true else onBack()
    }

    fun trySave(): String? {
        val hole = if (holeValue in listOf(9, 18, 27, 36)) holeValue else customHole.toIntOrNull() ?: 0
        if (hole <= 0) { message = "홀 정보를 입력하세요."; return null }
        if (courseName.isEmpty()) { message = "구장명을 입력하세요."; return null }
        if (courseName.length > 10) { message = "구장명은 10자 이내로 입력하세요."; return null }
        if (courseName.contains("파크")) { message = "'파크' 단어가 포함되어 있습니다."; return null }
        if (gameDate.isEmpty()) { message = "경기일을 선택하세요."; return null }
        val names = players.map(CleanRules::cleanPlayerName)
        val record = ScoreRecord(
            gameId = IdGen.gameId(DateTimeUtils.yearOf(gameDate)),
            courseName = CleanRules.cleanCourseName(courseName),
            gameDate = gameDate,
            region = region,
            totalHoles = hole,
            distances = selectedPlace?.courses?.flatMap { it.distances.dropLast(1) }.orEmpty(),
            pars = selectedPlace?.courses?.flatMap { it.pars.dropLast(1) }.orEmpty(),
            scoreType = scoreType,
            players = names.map { PlayerScore(name = it) }.toMutableList(),
            updateDate = DateTimeUtils.now()
        )
        val year = DateTimeUtils.yearOf(gameDate)
        val list = storage.loadRecords(year).toMutableList()
        list.add(record)
        storage.saveRecords(year, list)
        return record.gameId
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "신규 경기",
                onNavigate = ::backOrConfirm
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("구장명 ", fontSize = 14.sp)
                            Row(
                                modifier = Modifier.weight(1f).clickableNoRipple { onGoCourseList() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (courseName.isNotEmpty()) "$courseName 파크골프장" else "(구장 선택)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (courseName.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                OutlinedButton(onClick = onGoCourseList, modifier = Modifier.height(36.dp)) {
                                    Text("구장정보", fontSize = 12.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("경기일", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = { showDatePick = true },
                                modifier = Modifier.width(110.dp).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(DateTimeUtils.toDisplay(gameDate), fontSize = 11.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("지역", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = { showRegionDialog = true },
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(region.ifEmpty { "선택" }, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("홀", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row {
                            listOf(9, 18, 27).forEach { h ->
                                RadioH(selected = holeValue == h, label = "${h}홀") { holeValue = h; customHole = "" }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                        Row {
                            RadioH(selected = holeValue == 36, label = "36홀") { holeValue = 36; customHole = "" }
                            Spacer(modifier = Modifier.width(6.dp))
                            OtherHoleField(
                                selected = holeValue !in listOf(9, 18, 27, 36),
                                value = customHole,
                                onValueChange = { customHole = it },
                                onSelect = { holeValue = -1 }
                            )
                        }
                        ('A' until ('A' + numCourses)).toList().chunked(3).forEach { rowChips ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowChips.forEach { ch ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "${ch}코스(${courseColorLabel(ch - 'A')})",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 13.sp, maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Text("점수입력방식", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row {
                            RadioH(selected = scoreType == "A", label = "절대점수 (A)") { scoreType = "A" }
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioH(selected = scoreType == "R", label = "상대점수 (R)") { scoreType = "R" }
                        }
                    }
                }
            }

            item {
                Text("선수정보 (최대 4명, 선수명 5자이내)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            itemsIndexed(players) { idx, p ->
                Card {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                when {
                                    nickNames.isEmpty() -> message = "설정에서 선수 닉네임을 먼저 등록하세요."
                                    nickNames.size == 1 -> players[idx] = nickNames[0].take(5)
                                    else -> showNickDialog = idx
                                }
                            },
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("닉네임", fontSize = 10.sp, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NO.${idx + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = p,
                            onValueChange = { players[idx] = it.take(5) },
                            placeholder = { Text("선수명 (5자 이내)") },
                            colors = inputFieldColors(),
                            textStyle = InputTextStyle,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (players.size > 1) {
                            TextButton(onClick = { players.removeAt(idx) }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }

            item {
                if (players.size < 4) {
                    OutlinedButton(onClick = { players.add("") }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        Text("+ 추가")
                    }
                } else {
                    Text("선수는 최대 4명까지 추가할 수 있습니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Button(
                        onClick = {
                            val id = trySave()
                            if (id != null) onSaved(id, true)
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("저장", maxLines = 1)
                    }
                    TextButton(onClick = { onBack() }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("취소", maxLines = 1)
                    }
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
    if (showDatePick) {
        DatePickDialog(gameDate, onPick = { gameDate = it }, onDismiss = { showDatePick = false })
    }
    showNickDialog?.let { idx ->
        ComboDialog("닉네임", nickNames, "",
            onSelect = { players[idx] = it.take(5); showNickDialog = null },
            onDismiss = { showNickDialog = null })
    }
    message?.let { InfoMessage(it, onDismiss = { message = null }) }
    if (showBackConfirm) {
        ConfirmMessage("저장하고 이전화면으로 이동하시겠습니까?\n(아니오를 선택하면 저장하지 않고 이동합니다.)",
            onYes = {
                val id = trySave()
                if (id != null) {
                    showBackConfirm = false
                    onBack()
                }
            },
            onNo = {
                showBackConfirm = false
                onBack()
            }
        )
    }
}