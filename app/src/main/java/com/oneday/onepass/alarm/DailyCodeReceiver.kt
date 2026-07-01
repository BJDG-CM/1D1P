package com.oneday.onepass.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oneday.onepass.core.CodeGenerator
import com.oneday.onepass.data.SecureStore
import com.oneday.onepass.notify.Notifications
import java.time.LocalDate

/**
 * Fired by [AlarmScheduler] at 9:00 AM. It:
 *  1. draws a fresh, purely-random 4-digit code,
 *  2. stores it (encrypted) tagged with today's date,
 *  3. posts the notification showing the code,
 *  4. re-arms the alarm for the next day so the cycle repeats forever.
 */
class DailyCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Step 1 + 2: generate and persist.
        val code = CodeGenerator().generateCode()
        SecureStore.get(context).saveCode(code, LocalDate.now())

        // Step 3: notify (code is intentionally visible here).
        Notifications.showCode(context, code)

        // Step 4: always re-schedule for tomorrow 9:00.
        AlarmScheduler.scheduleNext(context)
    }

    companion object {
        const val ACTION_GENERATE = "com.oneday.onepass.action.GENERATE_CODE"
    }
}
