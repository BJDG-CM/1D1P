package com.oneday.onepass.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the daily alarm after a reboot. Exact alarms do not survive a reboot, so without this the
 * 9:00 code would stop being generated until the app is next opened.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> AlarmScheduler.scheduleNext(context)
        }
    }
}
