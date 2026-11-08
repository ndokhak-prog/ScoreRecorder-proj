package com.example.scorerecorder

import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scorerecorder.ui.theme.ScoreRecorderTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * 신규경기 지역 표시줄에서 '서울' 두 글자가 버튼 폭 안에 온전히 들어가는지 실제 레이아웃으로 검증.
 * (기존 '서' 한 글자만 보이던 문제 재발 방지용)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class NewGameRegionWidthTest {

    @Test
    fun seoulTwoCharsMustFitRegionButton() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var btnWdp = 0f
        var txtWdp = 0f

        val view = ComposeView(activity)
        view.setContent {
            ScoreRecorderTheme {
                val density = LocalDensity.current.density
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("경기일", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.width(110.dp).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("2026.09.30", fontSize = 11.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("지역", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .onGloballyPositioned { btnWdp = it.size.width / density },
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(
                                    "서울",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.onGloballyPositioned { txtWdp = it.size.width / density }
                                )
                            }
                        }
                    }
                }
            }
        }
        activity.setContentView(view)

        ShadowLooper.idleMainLooper()

        val dm = activity.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        view.measure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)
        ShadowLooper.idleMainLooper()

        // 버튼 내부(수평 패딩 4dp×2 제외) 폭이 "서울" 텍스트 폭보다 커야 온전히 표시됨
        println("신규경기 지역버튼폭=${btnWdp}dp, '서울'텍스트폭=${txtWdp}dp, 내부가용=${btnWdp - 8}dp")
        assertTrue("지역버튼이 측정되지 않음", btnWdp > 0)
        assertTrue("지역버튼 폭(${btnWdp}dp)이 '서울'(둘째 자리 포함)을 담을 만큼 넓지 않음(텍스트 ${txtWdp}dp)",
            btnWdp - 8 >= txtWdp + 30)
    }
}