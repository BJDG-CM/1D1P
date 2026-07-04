package com.oneday.onepass.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.oneday.onepass.core.CodeGenerator
import com.oneday.onepass.core.PasswordHasher
import java.time.LocalDate

/**
 * Single source of truth for everything persisted, backed by [EncryptedSharedPreferences].
 *
 * Nothing is ever written in plaintext:
 *  - the app password is stored only as salt + SHA-256 hash (see [PasswordHasher]);
 *  - the daily code is stored inside the AES-encrypted prefs file, tagged with the date it belongs
 *    to so a stale code from a previous day is never shown.
 */
class SecureStore private constructor(private val prefs: SharedPreferences) {

    // ---- Password ----

    val hasPassword: Boolean
        get() = prefs.contains(KEY_PW_HASH) && prefs.contains(KEY_PW_SALT)

    fun savePassword(hashed: PasswordHasher.Hashed) {
        prefs.edit()
            .putString(KEY_PW_SALT, hashed.saltHex)
            .putString(KEY_PW_HASH, hashed.hashHex)
            .apply()
    }

    fun verifyPassword(candidate: String, hasher: PasswordHasher = PasswordHasher()): Boolean {
        val salt = prefs.getString(KEY_PW_SALT, null) ?: return false
        val hash = prefs.getString(KEY_PW_HASH, null) ?: return false
        return hasher.verify(candidate, salt, hash)
    }

    // ---- Settings ----

    /** When false, the 9 AM alarm still generates/stores the code but posts no notification. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) { prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply() }

    /** When true, the code screen shows a manual "trigger now" test button. */
    var testButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEST_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_TEST_ENABLED, value).apply() }

    // ---- Daily code (kept as a per-date history) ----

    /** One day's generated code. */
    data class CodeEntry(val date: LocalDate, val code: String)

    /** The code for [today], or null if none has been generated for today yet. */
    fun codeForToday(today: LocalDate = LocalDate.now()): String? =
        prefs.getString(codeKey(today), null)

    /** Persists [code] under [date]. Each day is stored separately so past days are retained. */
    fun saveCode(code: String, date: LocalDate = LocalDate.now()) {
        require(code.length == CodeGenerator.LENGTH)
        prefs.edit()
            .putString(codeKey(date), code)
            .apply()
    }

    /**
     * All stored codes, newest day first. Reads every `code_yyyy-MM-dd` entry from the encrypted
     * prefs so the user can review previous days.
     */
    fun history(): List<CodeEntry> =
        prefs.all.mapNotNull { (key, value) ->
            if (value !is String) return@mapNotNull null
            val dateText = key.removePrefix(KEY_CODE_PREFIX)
            if (dateText == key) return@mapNotNull null // key had no prefix
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@mapNotNull null
            CodeEntry(date, value)
        }.sortedByDescending { it.date }

    private fun codeKey(date: LocalDate): String = KEY_CODE_PREFIX + date

    companion object {
        private const val FILE_NAME = "onepass_secure_prefs"

        private const val KEY_PW_SALT = "pw_salt"
        private const val KEY_PW_HASH = "pw_hash"
        private const val KEY_CODE_PREFIX = "code_"
        private const val KEY_NOTIF_ENABLED = "settings_notif_enabled"
        private const val KEY_TEST_ENABLED = "settings_test_enabled"

        @Volatile
        private var instance: SecureStore? = null

        fun get(context: Context): SecureStore =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(appContext: Context): SecureStore {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                appContext,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return SecureStore(prefs)
        }
    }
}
