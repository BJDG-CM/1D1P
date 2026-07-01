package com.oneday.onepass.ui

/**
 * Thin bundle of platform capabilities the UI needs but shouldn't reach for directly. Supplied by
 * [MainActivity], which owns the Android APIs (permission state, settings intents).
 */
data class SystemAccess(
    val isNotificationEnabled: () -> Boolean,
    val canScheduleExactAlarms: () -> Boolean,
    val openNotificationSettings: () -> Unit,
    val openExactAlarmSettings: () -> Unit,
)
