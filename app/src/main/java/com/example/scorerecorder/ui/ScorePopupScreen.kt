package com.example.scorerecorder.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScorePopupScreen(
    scoreType: String,
    initial: Int?,
    par: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
    onResetHole: () -> Unit = {},
    onCopyToAll: (Int?) -> Unit = {}
) {
    var selected by remember { mutableStateOf(initial) }
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "alpha"
    )
    val values: List<Int> = if (scoreType == "R") {
        val pv = (par ?: 4).coerceAtLeast(1)
        (-pv..pv).toList()
    } else {
        val pv = par ?: 0
        if (pv > 0) (1..(pv * 2)).toList() else (1..10).toList()
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(text = if (scoreType == "R") "상대점수 (기준타 대비)" else "절대점수 선택", fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                VoiceScoreMic(allowed = values) { v -> selected = v }
                values.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { v ->
                            val isSel = selected == v
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickableNoRipple { selected = v }.graphicsLayer { alpha = if (isSel) blinkAlpha else 1f }
                            ) {
                                Text(
                                    text = if (scoreType == "R" && v > 0) "+" + v.toString() else v.toString(),
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                if (selected != null) {
                    Text(
                        text = "선택값 : " + selected.toString(),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { selected = null; onResetHole() }) { Text("해당홀초기화") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onCopyToAll(selected) }) { Text("동일점수") }
                    TextButton(onClick = { onSelect(selected) }) { Text("닫기") }
                }
            }
        }
    )
}
