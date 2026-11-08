package com.example.scorerecorder.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.R
import com.example.scorerecorder.data.*

/**
 * 초기 화면 (최근기록 : 일자별 / 선수별)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    storage: Storage,
    onSettings: () -> Unit,
    onNewGame: () -> Unit,
    onRecordList: () -> Unit,
    onCourseList: () -> Unit,
    onRecordDetail: (gameId: String) -> Unit
) {
    var settings by remember { mutableStateOf(storage.loadSettings()) }
    val viewType = settings.latestViewType
    val isDaily = viewType == 1

    val (fr, to) = settings.latestPeriodRange()
    val records = remember(fr, to, viewType, settings) {
        if (viewType == 1) storage.loadRecordsInPeriod(fr, to)
        else storage.loadRecordsInPeriod("000000", "999999")
    }
    val nickname = settings.nickname

    Scaffold(
        topBar = {
            AppTopBar(
                title = "N독학러(@ndokhak) 파크골프",
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "설정")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onNewGame, modifier = Modifier.weight(1f)) {
                        Text("신규 경기", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                    }
                    OutlinedButton(onClick = onRecordList, modifier = Modifier.weight(1f)) {
                        Text("기록 목록", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                    }
                    OutlinedButton(onClick = onCourseList, modifier = Modifier.weight(1f)) {
                        Text("구장 정보", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isDaily) {
                // 기간 / 닉네임
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        val periodText =
                            "기간 ${DateTimeUtils.parseYmDisplay(fr)} ~ ${DateTimeUtils.parseYmDisplay(to)}"
                        // 닉네임은 '프로' 포함 최대 7자리까지 잘리지 않게 표시 (자간/글자크기 조정)
                        val nickText =
                            if (nickname.isNotEmpty()) (if (nickname.contains("프로")) nickname else "${nickname} 프로님")
                            else "선수 미등록"
                        val oneLine = "$periodText  $nickText"
                        Text(
                            oneLine,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            letterSpacing = (-0.5).sp,
                            fontSize = when {
                                oneLine.length >= 30 -> 12.sp
                                oneLine.length >= 26 -> 13.sp
                                else -> 14.sp
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (nickname.isEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isDaily) {
                DailyRecordView(records, nickname, onRecordDetail)
            } else {
                PlayerRecordView(storage, records)
            }

            Spacer(modifier = Modifier.height(16.dp))
            NdokhakBanner()
        }
        }
    }
}

/** 하단 N독학러 배너 + 유튜브 채널 하이퍼링크 */
@Composable
private fun NdokhakBanner() {
    val context = LocalContext.current
    val youtubeUrl = "https://www.youtube.com/@ndokhak"
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ndokhak_banner),
            contentDescription = "N독학러 유튜브 채널",
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .aspectRatio(1678f / 937f)
                .clickableNoRipple {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)))
                    } catch (_: Exception) {
                    }
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "유튜브채널 N독학러",
            color = Color(0xFF1A237E),
            textDecoration = TextDecoration.Underline,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickableNoRipple {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)))
                } catch (_: Exception) {
                }
            }
        )
    }
}

/** 일자별 뷰 */
@Composable
private fun DailyRecordView(
    records: List<ScoreRecord>,
    nickname: String,
    onRecordDetail: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("일자별", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("상세", weight = 0.5f)
                HeaderCell("경기일", weight = 1.1f)
                Spacer(Modifier.width(6.dp))
                HeaderCell("구장명", weight = 1.7f)
                HeaderCell("홀", weight = 0.7f)
                HeaderCell("점수", weight = 0.7f, navy = true)
                HeaderCell("기준", weight = 0.7f)
                HeaderCell("차이", weight = 0.7f)
            }
            Divider()
            if (records.isEmpty()) {
                EmptyRow()
            } else {
                records.forEach { rec ->
                    val player = pickPlayer(rec, nickname)
                    val base = rec.pars.take(rec.totalHoles).sum()
                    val score = player?.total ?: 0
                    val diff = score - base
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { onRecordDetail(rec.gameId) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cell("▸", weight = 0.5f, fs = 20f)
                        Cell(DateTimeUtils.toDisplayMd(rec.gameDate), weight = 1.1f, align = TextAlign.Right)
                        Spacer(Modifier.width(6.dp))
                        Cell(rec.courseName, weight = 1.7f, align = TextAlign.Start)
                        Cell("${rec.totalHoles}홀", weight = 0.7f, align = TextAlign.Right)
                        Cell("$score", weight = 0.7f, align = TextAlign.Right, navy = true)
                        Cell("$base", weight = 0.7f, align = TextAlign.Right)
                        Cell(diffWithSign(diff), weight = 0.7f, align = TextAlign.Right)
                    }
                }
            }
        }
    }
}

/** 선수별 뷰 */
@Composable
private fun PlayerRecordView(
    storage: Storage,
    records: List<ScoreRecord>
) {
    val latest = records.firstOrNull() ?: storage.latestRecord()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("선수별", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Card {
            if (latest == null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("경기 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderCell("경기일", weight = 1.2f)
                    HeaderCell("구장명", weight = 1.4f)
                    HeaderCell("홀", weight = 0.7f)
                    HeaderCell("기준", weight = 0.8f)
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cell(DateTimeUtils.toDisplay(latest.gameDate), weight = 1.2f)
                    Cell(latest.courseName, weight = 1.4f)
                    Cell("${latest.totalHoles}홀", weight = 0.7f)
                    Cell("${latest.pars.take(latest.totalHoles).sum()}", weight = 0.8f)
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderCell("순위", weight = 0.5f)
                    HeaderCell("선수명", weight = 1.6f)
                    HeaderCell("점수", weight = 0.8f)
                    HeaderCell("차이", weight = 0.8f)
                }
                Divider()
                val base = latest.pars.take(latest.totalHoles).sum()
                // 정렬기준: 차이(=점수-기준) 오름차순. 동타는 동순위(1,2,2,4)
                val ranked = latest.players.indices
                    .filter { !latest.players[it].excluded }
                    .sortedBy { latest.players[it].total }
                val orderedRanks = ScoreEngine.competitionRanks(ranked.map { latest.players[it].total })
                val rankByIndex = HashMap<Int, Int>()
                ranked.forEachIndexed { i, idx -> rankByIndex[idx] = orderedRanks[i] }
                val displayOrder = latest.players.indices.sortedBy {
                    if (latest.players[it].excluded) Int.MAX_VALUE else latest.players[it].total
                }
                displayOrder.forEach { i ->
                    val p = latest.players[i]
                    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cell(if (p.excluded) "제외" else "${rankByIndex[i]}", weight = 0.5f)
                        // 선수명은 닉네임만 표시 ('프로님' 붙이지 않음)
                        Cell(p.name, weight = 1.6f)
                        Cell("${p.total}", weight = 0.8f)
                        Cell("${diffWithSign(p.total - base)}", weight = 0.8f)
                    }
                }
            }
        }
    }
}

private fun pickPlayer(rec: ScoreRecord, nickname: String): PlayerScore? {
    val players = rec.players
    if (players.isEmpty()) return null
    players.firstOrNull { it.name == nickname }?.let { return it }
    return players.first()
}

private fun diffWithSign(v: Int): String = if (v > 0) "+$v" else "$v"

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float = 1f, navy: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = if (navy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RowScope.Cell(
    text: String,
    weight: Float = 1f,
    align: TextAlign = TextAlign.Center,
    navy: Boolean = false,
    fs: Float = 12f
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        fontSize = fs.sp,
        fontWeight = if (navy) FontWeight.Bold else FontWeight.Normal,
        textAlign = align,
        color = if (navy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1
    )
}

@Composable
private fun EmptyRow() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(
            "조회 조건에 해당하는 경기 기록이 없습니다.",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}