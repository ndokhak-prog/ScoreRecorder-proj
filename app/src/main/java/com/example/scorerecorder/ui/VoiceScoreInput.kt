package com.example.scorerecorder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/** 골프 점수 음성인식에서 사용하는 골프 용어 */
private val GOLF_TERMS = mapOf(
    "알바트로스" to -4, "도전" to -4,
    "이글" to -3, "트리플" to -3, "트리플보" to -3, "에이스" to -3, "홀인원" to -3, "원홀" to -3,
    "보리" to -2, "에그" to -2, "에거스" to -2, "더블" to -2,
    "버디" to -1, "언더" to -1, "언더파" to -1, "진입" to -1,
    "파" to 0, "파스" to 0, "제로" to 0, "기준" to 0, "기준타" to 0
)

/** 한국어 숫자어 (인식기가 숫자로 못 읽는 경우 대체) */
private val KO_DIGITS = mapOf(
    "영" to 0, "공" to 0, "오공" to 0,
    "일" to 1, "한" to 1, "하나" to 1, "원" to 1,
    "이" to 2, "두" to 2, "투" to 2, "쇼" to 2,
    "삼" to 3, "세이" to 3,
    "사" to 4, "포" to 4, "포오" to 4, "파이브" to 5, "오" to 5,
    "육" to 6, "칠" to 7, "팔" to 8, "구" to 9
)

/** 영어 숫자 및 오인식 빈번한 표기 */
private val EN_DIGITS = mapOf(
    "zero" to 0, "oh" to 0, "one" to 1, "won" to 1, "two" to 2, "to" to 2, "too" to 2,
    "three" to 3, "tree" to 3, "four" to 4, "for" to 4, "five" to 5,
    "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9
)

enum class AcceptResult { ACCEPTED, OUT_OF_RANGE, UNPARSED }

/**
 * 음성인식 후보 목록에서 선택 가능한 점수를 찾아낸다.
 * 인식기는 같은 말도 여러 표기로 돌려주므로 후보를 순서대로 모두 검사한다.
 */
fun pickVoiceScore(candidates: List<String>, allowed: List<Int>): Pair<AcceptResult, Int> {
    var firstParsed: Int? = null
    for (c in candidates) {
        val v = parseVoiceScore(c) ?: continue
        if (firstParsed == null) firstParsed = v
        if (allowed.contains(v)) return AcceptResult.ACCEPTED to v
    }
    return if (firstParsed != null) AcceptResult.OUT_OF_RANGE to firstParsed!!
    else AcceptResult.UNPARSED to 0
}

/**
 * 음성인식 결과 문자열을 홀 점수(Int)로 변환한다.
 * 인식 결과는 기기/앱마다 달라지므로 호출자가 허용 범위([pickVoiceScore]) 검증 후 사용해야 한다.
 *
 * 지원 형태: "4", "사", "타이4", "마이너스1", "빼 2", "+1", "버디", "이글", "zero", "three"
 * 파싱 불가는 경우 null 반환.
 */
fun parseVoiceScore(raw: String): Int? {
    val norm = raw.lowercase()
        .replace("\u00a0", " ")
        .replace("마이너스", "-")
        .replace("빼", "-")
        .replace("플러스", "+")
    val tokens = norm.split(Regex("[^0-9a-z가-힣+-]+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null

    var sign = 1
    for (t in tokens) {
        val neg = t.startsWith("-")
        val pos = t.startsWith("+")
        val core = t.trimStart('+', '-')
        if (core.isEmpty()) {
            if (neg) sign = -1
            if (pos) sign = 1
            continue
        }
        // "4점", "타이3" 처럼 토큰 안에 다른 문자가 섞인 경우 숫자부분만 사용한다
        val digits = core.dropWhile { !it.isDigit() }.takeWhile { it.isDigit() }
        val v = core.toIntOrNull()
            ?: digits.toIntOrNull()
            ?: GOLF_TERMS[core]
            ?: KO_DIGITS[core]
            ?: EN_DIGITS[core]
            ?: continue
        val s = when {
            neg -> -1
            pos -> 1
            else -> sign
        }
        return if (v < 0) v else s * v
    }
    return null
}

/** SpeechRecognizer 래퍼 (메인 스레드에서만 사용) */
private class VoiceRecognizer(
    private val tryAccept: (List<String>) -> AcceptResult,
    private val onFail: (String) -> Unit
) {
    var listening by mutableStateOf(false)
        private set
    var heard by mutableStateOf("")
        private set

    private var sr: SpeechRecognizer? = null
    private var appContext: Context? = null
    private var preferOffline = false
    private var attempt = 0
    private var lastPartial = ""

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { heard = "" }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            firstResult(partialResults)?.let {
                heard = it
                lastPartial = it
            }
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.toList().orEmpty()
            val cands = if (list.isEmpty() && lastPartial.isNotBlank()) listOf(lastPartial) else list
            when (tryAccept(cands)) {
                AcceptResult.ACCEPTED -> return
                AcceptResult.OUT_OF_RANGE -> {
                    onFail("")
                    return
                }
                AcceptResult.UNPARSED -> retryOrFail("점수로 읽지 못했습니다")
            }
        }

        override fun onError(error: Int) {
            listening = false
            when (error) {
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    retryOrFail(errorMessage(error))
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    retryOrFail(errorMessage(error))
                else -> onFail(errorMessage(error))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun firstResult(b: Bundle?): String? =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "마이크 오류가 발생했습니다."
        SpeechRecognizer.ERROR_CLIENT -> "인식 요청이 실패했습니다."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "네트워크를 사용할 수 없어 오프라인 인식으로 재시도했으나 실패했습니다."
        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다. 정수를 또렷하게 말해 주세요."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기가 사용 중입니다. 잠시 후 다시 시도해 주세요."
        SpeechRecognizer.ERROR_SERVER -> "인식 서버 오류가 발생했습니다."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말하는 소리가 없습니다."
        else -> "음성인식에 실패했습니다."
    }

    /** 네트워크 실패 시 오프라인 전환, 그 외 오류는 횟수 제한 내에서 재시도 */
    private fun retryOrFail(msg: String) {
        val ctx = appContext
        if (ctx == null) {
            onFail(msg)
            return
        }
        if (!preferOffline && attempt < 1) {
            preferOffline = true
            attempt++
            listen(ctx)
            return
        }
        if (attempt < 2) {
            attempt++
            listen(ctx)
            return
        }
        onFail(msg)
    }

    fun start(context: Context) {
        appContext = context.applicationContext
        attempt = 0
        preferOffline = false
        lastPartial = ""
        listen(appContext!!)
    }

    private fun listen(context: Context) {
        release()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onFail("이 기기에서는 음성인식을 사용할 수 없습니다.")
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(listener)
        sr = recognizer
        listening = true
        heard = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, preferOffline)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH", 1200L)
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH", 1800L)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH", 1500L)
        }
        runCatching { recognizer.startListening(intent) }
            .onFailure {
                listening = false
                onFail("음성인식을 시작할 수 없습니다.")
            }
    }

    fun stop() {
        runCatching { sr?.stopListening() }
        listening = false
    }

    fun release() {
        runCatching { sr?.cancel() }
        runCatching { sr?.destroy() }
        sr = null
        listening = false
    }
}

/** 마이크 아이콘 (아이콘 라이브러리 의존성 없이 Canvas로 직접 그린다) */
@Composable
private fun MicIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val stroke = w * 0.085f
        val capW = w * 0.32f
        val capTop = h * 0.10f
        val capBottom = h * 0.56f

        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - capW / 2f, capTop),
            size = Size(capW, capBottom - capTop),
            cornerRadius = CornerRadius(capW / 2f)
        )

        val arcLeft = w * 0.20f
        val arcRight = w * 0.80f
        val arcBottom = h * 0.66f
        val ry = h * 0.27f
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcBottom - 2f * ry),
            size = Size(arcRight - arcLeft, 2f * ry),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(cx, arcBottom),
            end = Offset(cx, h * 0.84f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.86f),
            end = Offset(w * 0.68f, h * 0.86f),
            strokeWidth = stroke
        )
    }
}

/**
 * 홀 점수 음성입력 마이크 버튼.
 * 마이크를 누르면 한국어 음성인식을 수행하고, 점수로 해석되면 onScore 로 전달한다.
 * 인식 후보를 모두 검사해 허용 범위에 들어가는 값을 고르며, 실패 시 오프라인 전환·재시도를 수행한다.
 */
@Composable
fun VoiceScoreMic(
    allowed: List<Int>,
    modifier: Modifier = Modifier,
    onScore: (Int) -> Unit
) {
    val context = LocalContext.current
    var failMsg by remember { mutableStateOf<String?>(null) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val recognizer = remember {
        VoiceRecognizer(
            tryAccept = { candidates ->
                val (result, value) = pickVoiceScore(candidates, allowed)
                when (result) {
                    AcceptResult.ACCEPTED -> {
                        failMsg = null
                        onScore(value)
                    }
                    AcceptResult.OUT_OF_RANGE -> failMsg = "선택할 수 없는 값입니다: $value"
                    AcceptResult.UNPARSED -> failMsg = null
                }
                result
            },
            onFail = { msg -> if (msg.isNotEmpty()) failMsg = msg }
        )
    }

    fun begin() {
        failMsg = null
        recognizer.start(context)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) begin() else failMsg = "마이크 권한이 필요합니다."
    }

    DisposableEffect(Unit) {
        onDispose { recognizer.release() }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = {
                if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
                else if (recognizer.listening) recognizer.stop() else begin()
            },
            modifier = Modifier.size(36.dp)
        ) {
            MicIcon(
                tint = if (recognizer.listening) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = when {
                failMsg != null -> failMsg!!
                recognizer.listening ->
                    if (recognizer.heard.isBlank()) "말해 주세요…" else "\"${recognizer.heard}\" 인식 중"
                else -> "마이크를 누르고 정수를 말하세요 (예: \"4\", \"마이너스 1\", \"버디\")"
            },
            fontSize = 11.sp,
            maxLines = 2,
            color = if (failMsg != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}