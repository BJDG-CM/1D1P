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

    // ---- Daily code ----

    /** The code for [today], or null if none has been generated for today yet. */
    fun codeForToday(today: LocalDate = LocalDate.now()): String? {
        val storedDate = prefs.getString(KEY_CODE_DATE, null) ?: return null
        if (storedDate != today.toString()) return null
        return prefs.getString(KEY_CODE_VALUE, null)
    }

    /** Persists [code] as belonging to [date]. Stored inside the encrypted prefs. */
    fun saveCode(code: String, date: LocalDate = LocalDate.now()) {
        require(code.length == CodeGenerator.LENGTH)
        prefs.edit()
            .putString(KEY_CODE_DATE, date.toString())
            .putString(KEY_CODE_VALUE, code)
            .apply()
    }

    companion object {
        private const val FILE_NAME = "onepass_secure_prefs"

        private const val KEY_PW_SALT = "pw_salt"
        private const val KEY_PW_HASH = "pw_hash"
        private const val KEY_CODE_DATE = "code_date"
        private const val KEY_CODE_VALUE = "code_value"

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
