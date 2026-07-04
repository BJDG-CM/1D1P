package com.oneday.onepass.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oneday.onepass.R
import com.oneday.onepass.ui.AppViewModel
import kotlinx.coroutines.launch

/**
 * Shows today's 4-digit code (or a "not yet generated" message). Tapping the code copies it to the
 * clipboard with a snackbar confirmation. Each digit is rendered in its own box so large glyphs
 * never overlap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeScreen(
    vm: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.code_copied)
    val testFired = stringResource(R.string.test_fired)
    val testFiredMuted = stringResource(R.string.test_fired_muted)

    var code by remember { mutableStateOf(vm.todayCode()) }
    // Re-read when returning to the screen (the alarm may have produced a code meanwhile).
    LifecycleResumeEffect(Unit) {
        code = vm.todayCode()
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.code_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.history_title),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val current = code
            if (current == null) {
                Text(
                    text = stringResource(R.string.code_none),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                CodeDigits(
                    code = current,
                    onClick = {
                        copyToClipboard(context, current)
                        scope.launch { snackbarHost.showSnackbar(copied) }
                    },
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.code_tap_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (vm.testButtonEnabled) {
                Spacer(Modifier.height(36.dp))
                ElevatedButton(
                    onClick = {
                        code = vm.runTestTrigger()
                        scope.launch {
                            snackbarHost.showSnackbar(
                                if (vm.notificationsEnabled) testFired else testFiredMuted,
                            )
                        }
                    },
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.test_button))
                }
            }
        }
    }
}

/** Renders each digit of [code] in its own rounded box, guaranteeing even spacing and no overlap. */
@Composable
fun CodeDigits(
    code: String,
    onClick: (() -> Unit)? = null,
    boxSize: Int = 72,
    fontSize: Int = 44,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        code.forEach { digit ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(
                    modifier = Modifier.size(boxSize.dp, (boxSize * 1.28f).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = digit.toString(),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("today_code", code))
}
