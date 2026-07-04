package com.oneday.onepass.alarm

import android.content.Context
import com.oneday.onepass.core.CodeGenerator
import com.oneday.onepass.data.SecureStore
import com.oneday.onepass.notify.Notifications
import java.time.LocalDate

/**
 * The single code-generation routine shared by the real 9 AM alarm ([DailyCodeReceiver]) and the
 * manual test button, so both behave identically.
 *
 * The notification is only posted when the in-app notification toggle is on — that is what makes
 * "notifications off ⇒ no ring at 9 AM" work. The code is always generated and stored regardless,
 * so it still appears in-app and in the history.
 */
object DailyCodeWorker {

    /**
     * @param reschedule true for the real alarm (re-arms tomorrow's alarm); false for a test press.
     * @return the newly generated code.
     */
    fun run(context: Context, reschedule: Boolean): String {
        val store = SecureStore.get(context)
        val code = CodeGenerator().generateCode()
        store.saveCode(code, LocalDate.now())

        if (store.notificationsEnabled) {
            Notifications.showCode(context, code)
        }
        if (reschedule) {
            AlarmScheduler.scheduleNext(context)
        }
        return code
    }
}
