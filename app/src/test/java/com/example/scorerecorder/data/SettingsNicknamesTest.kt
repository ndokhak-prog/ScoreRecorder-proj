package com.example.scorerecorder.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * 복수 닉네임 설정 저장/마이그레이션 테스트 (9차)
 */
@RunWith(RobolectricTestRunner::class)
class SettingsNicknamesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun legacyJson_onlyNickname_migratesToList() {
        val f = File(context.filesDir, Storage.SETTING_FILE)
        f.writeText("""{"settingId":"S1","nickname":"홍길동","latestViewType":2,"scoreType":"A","playerCount":1}""")

        val s = Storage(context).loadSettings()

        assertEquals(listOf("홍길동"), s.nicknames)
        assertEquals("홍길동", s.nickname)
        f.delete()
    }

    @Test
    fun legacyJson_emptyNickname_keepsEmptyList() {
        val f = File(context.filesDir, Storage.SETTING_FILE)
        f.writeText("""{"settingId":"S1","nickname":"","latestViewType":2,"scoreType":"A","playerCount":1}""")

        val s = Storage(context).loadSettings()

        assertEquals(0, s.nicknames.size)
        assertEquals("", s.nickname)
        f.delete()
    }

    @Test
    fun nicknamesPreserved_primaryKept() {
        val f = File(context.filesDir, Storage.SETTING_FILE)
        f.writeText("""{"settingId":"S1","nickname":"홍길동","nicknames":["박영희","홍길동","김철수"],"latestViewType":2,"scoreType":"A","playerCount":1}""")

        val s = Storage(context).loadSettings()

        assertEquals(listOf("박영희", "홍길동", "김철수"), s.nicknames)
        assertEquals("홍길동", s.nickname)
        f.delete()
    }

    @Test
    fun primaryNotInList_isAppended() {
        val f = File(context.filesDir, Storage.SETTING_FILE)
        f.writeText("""{"settingId":"S1","nickname":"홍길동","nicknames":["박영희"],"latestViewType":2,"scoreType":"A","playerCount":1}""")

        val s = Storage(context).loadSettings()

        assertEquals(listOf("박영희", "홍길동"), s.nicknames)
        assertEquals("홍길동", s.nickname)
        f.delete()
    }
}