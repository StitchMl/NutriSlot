package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditSlotDialog(
    dialogUi: EditSlotDialogUi,
    onDismiss: () -> Unit,
    onSave: (mealText: String, nutritionText: String) -> Unit,
    onReset: () -> Unit
) {
    var mealText by remember(dialogUi.slotId) { mutableStateOf(dialogUi.mealText) }
    var nutritionText by remember(dialogUi.slotId) { mutableStateOf(dialogUi.nutritionText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${dialogUi.dayLabel} • ${dialogUi.mealSlotLabel}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = mealText,
                    onValueChange = { mealText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cose consumate") },
                    minLines = 4
                )
            }
        },
        confirmButton = {
            IconButton(onClick = { onSave(mealText, nutritionText) }) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Salva",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Ripristina")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi")
                }
            }
        }
    )
}