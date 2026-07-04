package com.oneday.onepass.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oneday.onepass.R
import com.oneday.onepass.ui.AppViewModel
import com.oneday.onepass.ui.SystemAccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    system: SystemAccess,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
) {
    // Permission state can change while the user is in the OS settings screen, so re-read on resume.
    var notifEnabled by remember { mutableStateOf(system.isNotificationEnabled()) }
    var exactEnabled by remember { mutableStateOf(system.canScheduleExactAlarms()) }
    LifecycleResumeEffect(Unit) {
        notifEnabled = system.isNotificationEnabled()
        exactEnabled = system.canScheduleExactAlarms()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilledTonalButton(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_change_pw))
            }

            ToggleCard(
                title = stringResource(R.string.settings_notif_toggle),
                subtitle = stringResource(R.string.settings_notif_toggle_desc),
                checked = vm.notificationsEnabled,
                onCheckedChange = { vm.updateNotificationsEnabled(it) },
            )

            ToggleCard(
                title = stringResource(R.string.settings_test_toggle),
                subtitle = stringResource(R.string.settings_test_toggle_desc),
                checked = vm.testButtonEnabled,
                onCheckedChange = { vm.updateTestButtonEnabled(it) },
            )

            PermissionCard(
                label = stringResource(R.string.settings_notif_status),
                granted = notifEnabled,
                actionLabel = stringResource(R.string.settings_open_notif),
                onAction = system.openNotificationSettings,
            )

            PermissionCard(
                label = stringResource(R.string.settings_exact_alarm),
                granted = exactEnabled,
                actionLabel = stringResource(R.string.settings_open_exact),
                onAction = system.openExactAlarmSettings,
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(0.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PermissionCard(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(0.dp))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(if (granted) R.string.granted else R.string.denied),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}
