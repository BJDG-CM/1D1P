package com.oneday.onepass.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable full-screen PIN entry: a title, subtitle, the dot indicator, an optional error line, and
 * the numeric keypad. The field is internally managed but cleared whenever [resetSignal] changes,
 * which lets callers reset it (e.g. after a wrong attempt or when advancing to the next step).
 *
 * [onSubmit] fires once the entered digits reach [length].
 */
@Composable
fun PinScreen(
    title: String,
    subtitle: String,
    length: Int,
    errorText: String?,
    resetSignal: Int,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    var pin by remember(resetSignal) { mutableStateOf("") }

    LaunchedEffect(pin, resetSignal) {
        if (pin.length == length) {
            onSubmit(pin)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        header()

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        PinDots(filled = pin.length, length = length, isError = errorText != null)

        Spacer(Modifier.height(14.dp))
        Text(
            text = errorText ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        NumericKeypad(
            onDigit = { c -> if (pin.length < length) pin += c },
            onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        )
        Spacer(Modifier.height(24.dp))
    }
}
