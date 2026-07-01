package com.oneday.onepass.ui

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneday.onepass.alarm.AlarmScheduler
import com.oneday.onepass.ui.screens.ChangePasswordScreen
import com.oneday.onepass.ui.screens.CodeScreen
import com.oneday.onepass.ui.screens.LockScreen
import com.oneday.onepass.ui.screens.OnboardingScreen
import com.oneday.onepass.ui.screens.SettingsScreen
import com.oneday.onepass.ui.theme.OneDayOnePassTheme

private enum class Screen { ONBOARDING, LOCK, CODE, SETTINGS, CHANGE_PW }

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* status re-read on demand */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        maybeRequestNotificationPermission()

        setContent {
            OneDayOnePassTheme {
                val vm: AppViewModel = viewModel()
                AppNav(vm, buildSystemAccess())
            }
        }
    }

    @Composable
    private fun AppNav(vm: AppViewModel, system: SystemAccess) {
        var screen by remember {
            mutableStateOf(if (vm.hasPassword) Screen.LOCK else Screen.ONBOARDING)
        }

        when (screen) {
            Screen.ONBOARDING -> OnboardingScreen(
                vm = vm,
                onDone = { screen = Screen.CODE },
            )

            Screen.LOCK -> LockScreen(
                vm = vm,
                onUnlocked = { screen = Screen.CODE },
            )

            Screen.CODE -> CodeScreen(
                vm = vm,
                onOpenSettings = { screen = Screen.SETTINGS },
            )

            Screen.SETTINGS -> SettingsScreen(
                system = system,
                onBack = { screen = Screen.CODE },
                onChangePassword = { screen = Screen.CHANGE_PW },
            )

            Screen.CHANGE_PW -> ChangePasswordScreen(
                vm = vm,
                onBack = { screen = Screen.SETTINGS },
                onChanged = { screen = Screen.SETTINGS },
            )
        }
    }

    // ---- Platform capabilities handed to the UI ----

    private fun buildSystemAccess() = SystemAccess(
        isNotificationEnabled = { NotificationManagerCompat.from(this).areNotificationsEnabled() },
        canScheduleExactAlarms = {
            val am = getSystemService(AlarmManager::class.java)
            am != null && AlarmScheduler.canScheduleExact(am)
        },
        openNotificationSettings = { openNotificationSettings() },
        openExactAlarmSettings = { openExactAlarmSettings() },
    )

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = NotificationManagerCompat.from(this).areNotificationsEnabled()
            if (!granted) {
                requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } else {
            // Pre-Android 12 has no exact-alarm toggle; fall back to app details.
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                ),
            )
        }
    }
}
