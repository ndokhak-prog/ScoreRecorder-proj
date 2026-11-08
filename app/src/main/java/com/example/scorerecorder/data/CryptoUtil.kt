package com.example.scorerecorder.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 화면간 값 전달(POST 방식) 시 암/복호화 적용
 * AES-128/GCM, Base64
 */
object CryptoUtil {

    private const val KEY_BYTES = "ScOrERec07d2!@#$"  // 16 bytes
    private const val GCM_NONCE_LEN = 12

    private val keySpec by lazy { SecretKeySpec(KEY_BYTES.toByteArray(Charsets.UTF_8), "AES") }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val nonce = ByteArray(GCM_NONCE_LEN).also { java.security.SecureRandom().nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(nonce + ct, Base64.NO_WRAP)
        } catch (e: Exception) {
            plain
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        return try {
            val data = Base64.decode(cipherText, Base64.NO_WRAP)
            if (data.size <= GCM_NONCE_LEN) return cipherText
            val nonce = data.copyOfRange(0, GCM_NONCE_LEN)
            val ct = data.copyOfRange(GCM_NONCE_LEN, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }
}

/**
 * 화면간 전달 데이터 (post 방식). 실제 화면 이동 시
 * Json 직렬화 후 encrypt 하여 전달하고, 수신측에서 decrypt 후 역직렬화한다.
 */
data class NavBundle(
    val gameId: String = "",
    val isNew: Boolean = false,
    val courseId: String = "",
    val courseName: String = "",
    val courseHoles: Int = 0,
    val recordDate: String = "",
    val totalHoles: Int = 0,
    val scoreType: String = "",
    val playerNames: List<String> = emptyList(),
    val fromInitial: Boolean = false
) {
    fun toEncrypted(): String = CryptoUtil.encrypt(NavGson.toJson(this))
    companion object {
        fun decrypt(str: String): NavBundle =
            try { NavGson.fromJson(CryptoUtil.decrypt(str), NavBundle::class.java) }
            catch (e: Exception) { NavBundle() }
    }
}

object NavGson {
    private val gson = com.google.gson.Gson()
    fun toJson(o: Any): String = gson.toJson(o)
    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)
}