package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val parsedSections = remember(mealText) {
        parseMealSectionVisuals(mealText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${dialogUi.dayLabel} • ${dialogUi.mealSlotLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = slotTimeLabelFromLabel(dialogUi.mealSlotLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (parsedSections.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Anteprima rapida",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            parsedSections.take(3).forEachIndexed { index, section ->
                                if (index > 0) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Alternativa ${index + 1}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${section.visualInfo.emoji} ${section.lines.firstOrNull() ?: mealSemanticLabel(section.visualInfo.semanticKey)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        section.lines.drop(1).take(3).forEach { line ->
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = mealText,
                    onValueChange = { mealText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cose consumate") },
                    minLines = 6,
                    supportingText = {
                        Text("La preview sopra ignora automaticamente il blocco Nutrienti/tot kcal.")
                    }
                )

                OutlinedTextField(
                    value = nutritionText,
                    onValueChange = { nutritionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Riepilogo nutrienti") },
                    minLines = 2,
                    supportingText = {
                        Text("Questa parte resta salvabile, ma non viene usata per la lista della spesa.")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(mealText, nutritionText) }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Text(
                    text = " Salva",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                    Text(" Ripristina")
                }
                TextButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                    Text(" Chiudi")
                }
            }
        }
    )
}

private fun slotTimeLabelFromLabel(mealSlotLabel: String): String {
    return when (mealSlotLabel.trim().lowercase()) {
        "colazione" -> "07:30"
        "spuntino mattina" -> "10:30"
        "pranzo" -> "13:00"
        "spuntino pomeridiano" -> "16:30"
        "cena" -> "20:00"
        else -> ""
    }
}