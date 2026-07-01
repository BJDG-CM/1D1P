package com.oneday.onepass.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oneday.onepass.R
import com.oneday.onepass.core.PasswordHasher
import com.oneday.onepass.ui.AppViewModel
import com.oneday.onepass.ui.components.PinScreen

private const val PIN_LENGTH = PasswordHasher.PASSWORD_LENGTH

/** First-run screen: set the 8-digit password, entered twice for confirmation. */
@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var reset by remember { mutableIntStateOf(0) }

    val mismatch = stringResource(R.string.onboarding_mismatch)

    PinScreen(
        title = stringResource(R.string.onboarding_title),
        subtitle = if (confirming) {
            stringResource(R.string.onboarding_confirm_desc)
        } else {
            stringResource(R.string.onboarding_desc)
        },
        length = PIN_LENGTH,
        errorText = error,
        resetSignal = reset,
        onSubmit = { entered ->
            if (!confirming) {
                first = entered
                confirming = true
                error = null
                reset++
            } else if (entered == first) {
                vm.setInitialPassword(first)
                onDone()
            } else {
                error = mismatch
                first = ""
                confirming = false
                reset++
            }
        },
    )
}

/** Gate in front of the code screen: enter the password to proceed. */
@Composable
fun LockScreen(vm: AppViewModel, onUnlocked: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    var reset by remember { mutableIntStateOf(0) }
    val wrong = stringResource(R.string.lock_wrong)

    PinScreen(
        title = stringResource(R.string.lock_title),
        subtitle = stringResource(R.string.lock_desc),
        length = PIN_LENGTH,
        errorText = error,
        resetSignal = reset,
        onSubmit = { entered ->
            if (vm.unlock(entered)) {
                onUnlocked()
            } else {
                error = wrong
                reset++
            }
        },
    )
}

/** Change password: confirm the current one, then enter and confirm a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(vm: AppViewModel, onBack: () -> Unit, onChanged: () -> Unit) {
    // 0 = enter current, 1 = enter new, 2 = confirm new
    var step by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var reset by remember { mutableIntStateOf(0) }

    val wrong = stringResource(R.string.lock_wrong)
    val mismatch = stringResource(R.string.onboarding_mismatch)

    val subtitle = when (step) {
        0 -> stringResource(R.string.change_current)
        1 -> stringResource(R.string.change_new)
        else -> stringResource(R.string.change_confirm)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_change_pw)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
            )
        },
    ) { inner ->
        PinScreen(
            title = stringResource(R.string.settings_change_pw),
            subtitle = subtitle,
            length = PIN_LENGTH,
            errorText = error,
            resetSignal = reset,
            modifier = Modifier.padding(inner),
            onSubmit = { entered ->
                when (step) {
                    0 -> if (vm.unlock(entered)) {
                        current = entered
                        step = 1
                        error = null
                        reset++
                    } else {
                        error = wrong
                        reset++
                    }

                    1 -> {
                        newPassword = entered
                        step = 2
                        error = null
                        reset++
                    }

                    else -> if (entered == newPassword) {
                        vm.changePassword(current, newPassword)
                        onChanged()
                    } else {
                        error = mismatch
                        newPassword = ""
                        step = 1
                        reset++
                    }
                }
            },
        )
    }
}
