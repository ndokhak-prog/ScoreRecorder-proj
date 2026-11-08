package com.example.scorerecorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.data.*

/**
 * 기록 목록 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    storage: Storage,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onNewGame: () -> Unit
) {
    // 초기화면(MainScreen)과 동일한 규칙으로 설정에서 기간 초기값을 가져온다
    val settings = remember { storage.loadSettings() }
    val (initFr, initTo) = remember(settings) { settings.latestPeriodRange() }
    var fr by remember { mutableStateOf(initFr) }
    var to by remember { mutableStateOf(initTo) }
    var playerName by remember { mutableStateOf("") }

    var showFr by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    var dataVersion by remember { mutableStateOf(0) }
    var deleteTarget by remember { mutableStateOf<ScoreRecord?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val records = remember(fr, to, playerName, dataVersion) {
        storage.loadRecordsInPeriod(fr, to)
            .filter { playerName.isBlank() || it.players.any { p -> p.name == playerName } }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "기록 목록",
                onNavigate = onBack
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { showFr = true }, modifier = Modifier.height(40.dp)) { Text(DateTimeUtils.parseYmDisplay(fr), fontSize = 12.sp) }
                Text("~", fontSize = 13.sp)
                OutlinedButton(onClick = { showTo = true }, modifier = Modifier.height(40.dp)) { Text(DateTimeUtils.parseYmDisplay(to), fontSize = 12.sp) }
                OutlinedButton(onClick = { showPlayer = true }, modifier = Modifier.height(40.dp)) {
                    Text(playerName.ifEmpty { "전체 선수" }, fontSize = 11.sp, maxLines = 1)
                }
            }
            Divider()

            if (records.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("해당 조건의 경기 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(records, key = { it.gameId }) { rec ->
                        val base = rec.pars.take(rec.totalHoles).sum()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickableNoRipple { onOpen(rec.gameId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${DateTimeUtils.toDisplay(rec.gameDate)}  ${rec.courseName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${rec.totalHoles}홀  기준타 $base  점수방식 ${if (rec.scoreType == "R") "상대(R)" else "절대(A)"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // 참가선수 타수 오름차순 (단, @제외 항목은 맨 뒤로), 한 행에 2명씩
                                rec.players
                                    .sortedWith(compareBy({ it.excluded }, { it.total }))
                                    .chunked(2)
                                    .forEach { pair ->
                                        Row(Modifier.fillMaxWidth()) {
                                            pair.forEach { p ->
                                                Text(
                                                    "${p.name}(${p.total}${if (p.excluded) "@제외" else ""})",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val best = rec.players.filterNot { it.excluded }.minByOrNull { it.total }
                                if (best != null) {
                                    Text("우승 ${best.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    OutlinedButton(
                                        onClick = { RecordImage.share(context, rec) },
                                        modifier = Modifier.defaultMinSize(minWidth = 0.dp, minHeight = 26.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("공유", fontSize = 9.sp, maxLines = 1)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        OutlinedButton(
                                            onClick = { message = RecordImage.saveToGallery(context, rec) },
                                            modifier = Modifier.width(62.dp).height(26.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            Text("이미지저장", fontSize = 9.sp, maxLines = 1)
                                        }
                                        TextButton(
                                            onClick = { deleteTarget = rec },
                                            modifier = Modifier.width(62.dp).height(26.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp)
                                        ) {
                                            Text("삭제", color = MaterialTheme.colorScheme.error, fontSize = 9.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("신규", maxLines = 1)
                }
            }
        }
        }
    }

    if (showFr) YearMonthPicker2(fr, onPick = { fr = it; showFr = false }, onDismiss = { showFr = false })
    if (showTo) YearMonthPicker2(to, onPick = { to = it; showTo = false }, onDismiss = { showTo = false })
    if (showPlayer) {
        ComboDialog(
            "선수명 (등록 선수)",
            storage.playerNamesInPeriod(fr, to).ifEmpty { listOf("") } + listOf("(전체)"),
            playerName,
            onSelect = { playerName = it.takeIf { v -> v != "(전체)" } ?: ""; showPlayer = false },
            onDismiss = { showPlayer = false }
        )
    }
    deleteTarget?.let { rec ->
        ConfirmMessage(
            text = "이 경기기록을 삭제하시겠습니까?",
            onYes = {
                storage.deleteRecord(rec.gameId)
                dataVersion++
                deleteTarget = null
            },
            onNo = { deleteTarget = null }
        )
    }
    message?.let { InfoMessage(it, onDismiss = { message = null }) }
}

/** yyyymm 선택 (현재월 초과 불가) - 재사용 */
@Composable
private fun YearMonthPicker2(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var ym by remember { mutableStateOf(current.ifEmpty { DateTimeUtils.currentYearMonth() }) }
    val currentYm = DateTimeUtils.currentYearMonth()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기간 선택") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { ym = DateTimeUtils.addMonths(ym, -1) }) { Text("◀") }
                Text(DateTimeUtils.parseYmDisplay(ym), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = { if (ym < currentYm) ym = DateTimeUtils.addMonths(ym, 1) }, enabled = ym < currentYm) { Text("▶") }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(ym) }, enabled = ym <= currentYm) { Text("선택") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}