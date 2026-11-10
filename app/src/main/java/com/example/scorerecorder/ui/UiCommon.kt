package com.example.scorerecorder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 통일된 상단 바
 * 화이트 서피스 + 1px 헤어라인, 딥네이비 제목 (Sovereign Modernism)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onNavigate: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.primary
            ),
            title = {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (onNavigate != null) {
                    IconButton(onClick = onNavigate) {
                        Text(
                            "←",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = actions
        )
        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * 기본 카드 — 화이트 + 16dp 라운드 + 1px 고스트 보더 (Level 1)
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content
    )
}

/**
 * 하단 액션 바 — 화이트 서피스 + 상단 헤어라인
 */
@Composable
fun AppBottomBar(
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** 입력 필드 통일 스타일 (입력 글자는 네이비/보통 굵기, 라벨은 회색) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun inputFieldColors(): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.secondary
)

/** 입력 글자 스타일 (본문보다 크고 굳게, 입력색과 구분) */
val InputTextStyle
    @Composable get() = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )

/** 통일된 섹션 카드 (네이비 굵은 섹션 제목) */
@Composable
fun AppSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

/** 상단 바(화면명) 아래 콘텐츠 (확대/축소 없이 좌측상단 고정) */
@Composable
fun ZoomableContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        content()
    }
}

/** 단순 텍스트 입력 다이얼로그 */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = inputFieldColors(),
                textStyle = InputTextStyle,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onConfirm(text) }) { Text("확인", fontSize = 16.sp) }
                TextButton(onClick = onDismiss) { Text("닫기", fontSize = 16.sp) }
            }
        }
    )
}

/** 라디오버튼 + 라벨(수직 가운데 정렬). label이 null이면 라디오버튼만 표시 */
@Composable
fun RadioH(selected: Boolean, label: String?, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        if (label != null) {
            Text(
                label,
                modifier = Modifier.clickableNoRipple { onClick() },
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

/** ripple 없는 click modifier */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) { onClick() }
}

/** 선택 목록 콤보(드롭다운) 다이얼로그 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboDialog(
    title: String,
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { item ->
                    val isSel = item == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { onSelect(item) }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            if (isSel) "●" else "○",
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기", fontSize = 16.sp) }
        }
    )
}

/** 높이 지정 가능한 컴팩트 입력필드 (라벨/안내문구 없는 얇은 textbox) */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    enabled: Boolean = true,
    placeholder: String = "",
    textAlign: TextAlign = TextAlign.Start,
    filter: (Char) -> Boolean = { true },
    maxLength: Int = 20,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    Box(
        modifier = modifier
            .height(height)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                placeholder,
                fontSize = 11.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(filter).take(maxLength)) },
            singleLine = true,
            enabled = enabled,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = textAlign,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** '기타' 홀 선택 Row: 라디오 + 상시 노출 숫자 textbox + '홀' (기타 선택 시에만 활성화) */
@Composable
fun OtherHoleField(
    selected: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    fieldHeight: Dp = 44.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioH(selected = selected, label = "기타") { onSelect() }
        Box(
            modifier = Modifier
                .width(width)
                .height(fieldHeight)
                .background(
                    if (selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    MaterialTheme.shapes.small
                )
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (selected) onValueChange(input.filter { it.isDigit() }.take(3))
                },
                enabled = selected,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("홀", fontSize = 13.sp)
    }
}

/** 숫자만 입력 가능한 OutlinedTextField */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLength: Int = 6
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }
            onValueChange(filtered.take(maxLength))
        },
        label = { Text(label) },
        colors = inputFieldColors(),
        textStyle = InputTextStyle,
        modifier = modifier,
        enabled = enabled,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
    )
}

/** 안내 메시지 */
@Composable
fun InfoMessage(
    text: String,
    onDismiss: () -> Unit,
    title: String = "알림"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1) },
        text = { Text(text, fontSize = 15.sp) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인", fontSize = 16.sp) } }
    )
}

/** 확인/취소 메시지 (예/아니오 순서, 글자 크게) */
@Composable
fun ConfirmMessage(
    text: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
    title: String = "확인"
) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1) },
        text = { Text(text, fontSize = 15.sp) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onYes) { Text("예", fontSize = 16.sp) }
                TextButton(onClick = onNo) { Text("아니오", fontSize = 16.sp) }
            }
        }
    )
}

/** 흰색 아이콘용 Surface 래퍼 (상단 버튼) */
@Composable
fun AppActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(text, fontSize = 13.sp)
    }
}

/** Material DatePicker 다이얼로그 (yyyymmdd) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(
    initial: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val mState = rememberDatePickerState(
        initialSelectedDateMillis = com.example.scorerecorder.data.DateTimeUtils.storedToMillis(initial)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                mState.selectedDateMillis?.let { onPick(com.example.scorerecorder.data.DateTimeUtils.millisToStored(it)) }
                onDismiss()
            }) { Text("선택") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    ) {
        DatePicker(state = mState)
    }
}