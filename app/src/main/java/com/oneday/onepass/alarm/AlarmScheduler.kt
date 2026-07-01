package com.oneday.onepass.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules the exact 9:00 AM trigger that produces each day's code.
 *
 * Strategy:
 *  - [scheduleNext] computes the next 9:00 (today if it hasn't passed, otherwise tomorrow) and arms
 *    an exact alarm.
 *  - After the alarm fires, [DailyCodeReceiver] calls [scheduleNext] again, so it perpetually
 *    re-arms for the following day.
 *  - We prefer [AlarmManager.setExactAndAllowWhileIdle] so the code is generated even in Doze.
 *    If the OS won't allow exact alarms (Android 12+ without permission), we fall back to an
 *    inexact allow-while-idle alarm rather than failing silently.
 */
object AlarmScheduler {

    const val TRIGGER_HOUR = 9
    const val TRIGGER_MINUTE = 0

    private const val REQUEST_CODE = 9001

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextTriggerMillis(LocalDateTime.now())
        val pending = buildPendingIntent(context)

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending,
            )
        } else {
            // Best effort without exact-alarm permission: still wakes from idle, just not to-the-minute.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending,
            )
        }
    }

    fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    /** Next occurrence of 9:00, as epoch millis, relative to [now]. */
    fun nextTriggerMillis(now: LocalDateTime): Long {
        var next = now.withHour(TRIGGER_HOUR)
            .withMinute(TRIGGER_MINUTE)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyCodeReceiver::class.java).apply {
            action = DailyCodeReceiver.ACTION_GENERATE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
