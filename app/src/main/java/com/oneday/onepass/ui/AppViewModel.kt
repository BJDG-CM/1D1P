package com.oneday.onepass.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
}
