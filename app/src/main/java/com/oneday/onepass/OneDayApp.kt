package com.oneday.onepass

import android.app.Application
import com.oneday.onepass.alarm.AlarmScheduler
import com.oneday.onepass.notify.Notifications

/**
 * Application entry point. Ensures the notification channel exists and that the daily 9:00 alarm is
 * armed every time the process starts (cheap and idempotent — arming replaces any existing alarm).
 */
class OneDayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        AlarmScheduler.scheduleNext(this)
    }
}
