package com.example.scorerecorder

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 화면 회전(가로/세로) 시 Activity 재생성 여부 검증.
 *
 * MainActivity가 orientation/screenSize 등 구성 변경을 직접 처리하도록 선언되어 있어야
 * 회전해도 Activity가 재생성되지 않고 Compose 상태(현재 화면, 입력값)가 그대로 유지된다.
 */
@RunWith(RobolectricTestRunner::class)
class RotationConfigTest {

    private fun configChanges(): Int {
        val context: Context = ApplicationProvider.getApplicationContext()
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java), 0
        )
        return info.configChanges
    }

    @Test
    fun mainActivity_handlesOrientationChange_soStateIsKept() {
        val flags = configChanges()
        assertTrue(
            "orientation 미선언 → 회전 시 Activity 재생성되어 화면이 초기화됨",
            flags and ActivityInfo.CONFIG_ORIENTATION != 0
        )
        assertTrue(
            "screenSize 미선언 → 회전 시 Activity 재생성됨",
            flags and ActivityInfo.CONFIG_SCREEN_SIZE != 0
        )
        assertTrue(
            "screenLayout 미선언",
            flags and ActivityInfo.CONFIG_SCREEN_LAYOUT != 0
        )
        assertTrue(
            "smallestScreenSize 미선언",
            flags and ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE != 0
        )
    }
}
