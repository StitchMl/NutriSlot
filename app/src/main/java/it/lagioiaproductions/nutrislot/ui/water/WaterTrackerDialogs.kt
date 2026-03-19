package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AmountDialog(
    title: String,
    currentValueMl: Int,
    confirmLabel: String,
    presets: List<Int>,
    initialInput: String,
    helperText: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember(initialInput) { mutableStateOf(initialInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Current: $currentValueMl ml",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PresetRow(
                    values = presets.take(3),
                    onSelected = { input = it.toString() }
                )

                if (presets.size > 3) {
                    PresetRow(
                        values = presets.drop(3),
                        onSelected = { input = it.toString() }
                    )
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { newValue ->
                        input = newValue.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = input.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onConfirm(parsed)
                    }
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReminderDialog(
    enabled: Boolean,
    intervalMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Int) -> Unit
) {
    var localEnabled by remember(enabled) { mutableStateOf(enabled) }
    var localIntervalText by remember(intervalMinutes) { mutableStateOf(intervalMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Water reminders")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (localEnabled) "Reminders enabled" else "Reminders disabled",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = localEnabled,
                        onCheckedChange = { localEnabled = it }
                    )
                }

                Text(
                    text = "Choose how often you want to be reminded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PresetRow(
                    values = listOf(30, 60, 90),
                    suffix = "min",
                    onSelected = { localIntervalText = it.toString() }
                )

                PresetRow(
                    values = listOf(120, 150, 180),
                    suffix = "min",
                    onSelected = { localIntervalText = it.toString() }
                )

                OutlinedTextField(
                    value = localIntervalText,
                    onValueChange = { value ->
                        localIntervalText = value.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Reminder interval (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = localIntervalText.toIntOrNull()?.coerceAtLeast(15) ?: 60
                    onConfirm(localEnabled, parsed)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResetWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset daily water")
        },
        text = {
            Text("This will reset the consumed amount to 0 ml.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PresetRow(
    values: List<Int>,
    suffix: String = "ml",
    onSelected: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            Surface(
                onClick = { onSelected(value) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$value $suffix",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}