package com.oneday.onepass.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fired by [AlarmScheduler] at 9:00 AM. Delegates to [DailyCodeWorker], which generates and stores
 * the code, posts the notification (if enabled), and re-arms tomorrow's alarm.
 */
class DailyCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        DailyCodeWorker.run(context, reschedule = true)
    }

    companion object {
        const val ACTION_GENERATE = "com.oneday.onepass.action.GENERATE_CODE"
    }
}
