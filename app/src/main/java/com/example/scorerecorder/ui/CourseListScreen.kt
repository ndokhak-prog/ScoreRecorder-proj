package com.example.scorerecorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.R
import com.example.scorerecorder.data.*

/**
 * 구장 목록을 구장명 기준으로 정렬 (오름차순/내림차순)
 */
fun sortCoursesByName(list: List<GolfCourse>, asc: Boolean): List<GolfCourse> {
    val key: (GolfCourse) -> String = { CleanRules.cleanCourseName(it.courseName) }
    return if (asc) list.sortedBy(key) else list.sortedByDescending(key)
}

/**
 * 구장정보 목록 화면 (ndokhak_place.json)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    storage: Storage,
    selectForNewGame: Boolean,
    onBack: () -> Unit,
    onAddCourse: () -> Unit,
    onEditCourse: (String) -> Unit,
    onSelectCourse: (GolfCourse) -> Unit,
    onRefresh: () -> Unit,
    onEvaluate: (courseId: String, courseName: String) -> Unit
) {
    val settings = remember { storage.loadSettings() }

    var queryType by remember { mutableStateOf(settings.courseQueryType) }
    var region by remember { mutableStateOf(settings.region) }
    var searchText by remember { mutableStateOf("") }
    var dataVersion by remember { mutableStateOf(0) }
    var sortAsc by remember { mutableStateOf(true) }

    val places = remember(dataVersion) { storage.loadPlaces() }

    // 구장평가 총평(점수가 입력된 평가만) — 구장Id > 구장명(정제) 순으로 매칭
    val evaluations = remember(dataVersion) {
        storage.loadEvaluations().filter { ev -> ev.scores.any { it != null } }
    }
    fun evaluationOf(course: GolfCourse): CourseEvaluation? {
        if (course.courseId.isNotBlank()) evaluations.lastOrNull { it.courseId == course.courseId }?.let { return it }
        val name = CleanRules.cleanCourseName(course.courseName)
        return evaluations.filter { CleanRules.cleanCourseName(it.courseName) == name }
            .maxByOrNull { it.updateDate }
    }

    val filtered = remember(queryType, region, searchText, dataVersion, sortAsc) {
        var list = places
        if (queryType == "지역명" && region.isNotEmpty()) {
            list = list.filter { it.region == region }
        }
        if (searchText.isNotBlank()) {
            val keyword = CleanRules.cleanParkKeyword(searchText)
            list = list.filter { CleanRules.cleanCourseName(it.courseName).contains(keyword) }
        }
        sortCoursesByName(list, sortAsc)
    }

    val selected = remember { mutableStateListOf<String>() }

    var showQueryDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var proceedSelect by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<Int?>(null) }

    fun emitSelect() {
        val picked = places.filter { it.courseId in selected }
        when {
            picked.size == 1 -> onSelectCourse(picked.first())
            else -> proceedSelect = true
        }
    }

    fun emitEvaluate() {
        val picked = places.filter { it.courseId in selected }
        if (picked.size == 1) onEvaluate(picked.first().courseId, picked.first().courseName)
        else message = "구장정보 선택 체크박스를 하나만 선택하세요. (1건만 선택 가능)"
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "구장 정보",
                onNavigate = onBack
            )
        }
    ) { padding ->
        ZoomableContent(Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("조건", fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { showQueryDialog = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(queryType, fontSize = 12.sp, maxLines = 1)
                    }
                    if (queryType == "지역명") {
                        Text("지역", fontSize = 12.sp)
                        OutlinedButton(
                            onClick = { showRegionDialog = true },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(region.ifEmpty { "전체" }, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { sortAsc = !sortAsc },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            if (sortAsc) "구장명 ↑" else "구장명 ↓",
                            fontSize = 12.sp, maxLines = 1
                        )
                    }
                }
                if (queryType == "구장명") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompactTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = "구장명 검색어",
                            modifier = Modifier.weight(1f),
                            height = 40.dp,
                            maxLength = 30
                        )
                        IconButton(onClick = {}) {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = "검색")
                        }
                    }
                }
            }

            Divider()

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Text("조회된 구장이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filtered) { _, c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableNoRipple {
                                    if (c.courseId in selected) selected.remove(c.courseId)
                                    else selected.add(c.courseId)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = c.courseId in selected,
                                onCheckedChange = { chk ->
                                    if (chk) selected.add(c.courseId) else selected.remove(c.courseId)
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text("${c.courseName} 파크골프장", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "지역: ${c.region}  /  코스 ${c.courses.size}개  /  휴장일: ${c.closedDays.ifEmpty { "-" }}  /  주소: ${c.location}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val courseEval = evaluationOf(c)
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (courseEval != null) {
                                    Text(
                                        "평가 ${"%.1f".format(courseEval.total)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(if (c.totalHoles > 0) "${c.totalHoles}홀" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            Divider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectForNewGame) {
                    Button(
                        onClick = ::emitSelect,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("선택", fontSize = 13.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onAddCourse,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("구장 추가", fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = ::emitEvaluate,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("구장평가", fontSize = 12.sp, maxLines = 1)
                    }
                } else {
                    OutlinedButton(
                        onClick = onAddCourse,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("구장 추가", fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        val picked = places.filter { it.courseId in selected }
                        if (picked.size == 1) onEditCourse(picked.first().courseId)
                        else message = "구장정보 선택 체크박스를 하나만 선택하세요. (1건만 선택 가능)"
                    }, modifier = Modifier.weight(1f).height(44.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("수정", fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = ::emitEvaluate,
                        modifier = Modifier.weight(1f).height(44.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("구장평가", fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        deleteConfirm = selected.size
                    }, modifier = Modifier.weight(1f).height(44.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))) {
                        Text("삭제", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        }
    }

    if (showQueryDialog) {
        ComboDialog("조회조건", Storage.QUERY_TYPES.split(","), queryType,
            onSelect = { queryType = it; showQueryDialog = false; searchText = "" },
            onDismiss = { showQueryDialog = false })
    }
    if (showRegionDialog) {
        ComboDialog("지역", listOf("전체") + Storage.REGIONS.split(","), region,
            onSelect = { region = it.takeIf { v -> v != "전체" } ?: ""; showRegionDialog = false },
            onDismiss = { showRegionDialog = false })
    }
    message?.let { InfoMessage(it, onDismiss = { message = null }) }

    if (proceedSelect) {
        AlertDialog(
            onDismissRequest = { proceedSelect = false },
            title = { Text("구장정보 선택") },
            text = { Text("구장정보 선택 체크박스를 하나만 선택하세요. (1건만 선택 가능)\n계속 진행하시면 선택 없이 구장관리 화면으로 이동합니다.") },
            confirmButton = {
                TextButton(onClick = {
                    proceedSelect = false
                    val picked = places.filter { it.courseId in selected }
                    if (picked.isEmpty()) onSelectCourse(GolfCourse()) else onSelectCourse(picked.first())
                }) { Text("계속진행") }
            },
            dismissButton = { TextButton(onClick = { proceedSelect = false }) { Text("취소") } }
        )
    }

    deleteConfirm?.let { cnt ->
        ConfirmMessage(
            text = "${cnt}개 선택하셨습니다. 구장 삭제하시겠습니까?",
            onYes = {
                val remain = places.filterNot { it.courseId in selected }
                storage.savePlaces(remain)
                selected.clear()
                dataVersion++
                deleteConfirm = null
                onRefresh()
            },
            onNo = { deleteConfirm = null }
        )
    }
}