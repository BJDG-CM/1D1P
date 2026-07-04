package com.oneday.onepass.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.oneday.onepass.alarm.DailyCodeWorker
import com.oneday.onepass.core.PasswordHasher
import com.oneday.onepass.data.SecureStore
import java.time.LocalDate

/**
 * Holds app-level state and mediates all access to [SecureStore]. Kept deliberately thin: the
 * security-sensitive logic lives in the pure [PasswordHasher] / CodeGenerator classes.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SecureStore.get(app)
    private val hasher = PasswordHasher()

    val hasPassword: Boolean get() = store.hasPassword

    // ---- Settings (Compose-observable) ----

    var notificationsEnabled by mutableStateOf(store.notificationsEnabled)
        private set

    var testButtonEnabled by mutableStateOf(store.testButtonEnabled)
        private set

    fun updateNotificationsEnabled(enabled: Boolean) {
        store.notificationsEnabled = enabled
        notificationsEnabled = enabled
    }

    fun updateTestButtonEnabled(enabled: Boolean) {
        store.testButtonEnabled = enabled
        testButtonEnabled = enabled
    }

    /**
     * Simulates the 9 AM trigger right now: generates and stores a code and (if notifications are
     * enabled) posts the notification. Returns the code so the UI can refresh.
     */
    fun runTestTrigger(): String = DailyCodeWorker.run(getApplication(), reschedule = false)

    /** Creates the initial password. Caller must have validated the format. */
    fun setInitialPassword(password: String): Boolean {
        if (!PasswordHasher.isValidPassword(password)) return false
        store.savePassword(hasher.hash(password))
        return true
    }

    /** @return true if [candidate] matches the stored password. */
    fun unlock(candidate: String): Boolean = store.verifyPassword(candidate, hasher)

    /** Verifies the current password, then replaces it. @return true on success. */
    fun changePassword(current: String, newPassword: String): Boolean {
        if (!store.verifyPassword(current, hasher)) return false
        if (!PasswordHasher.isValidPassword(newPassword)) return false
        store.savePassword(hasher.hash(newPassword))
        return true
    }

    /** Today's code, or null if it hasn't been generated yet (e.g. before 9 AM). */
    fun todayCode(): String? = store.codeForToday(LocalDate.now())

    /** Every past day's code, newest first, for the history screen. */
    fun history(): List<SecureStore.CodeEntry> = store.history()
}
