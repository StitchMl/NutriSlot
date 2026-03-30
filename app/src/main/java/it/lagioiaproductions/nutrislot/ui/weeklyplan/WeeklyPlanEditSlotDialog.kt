package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onSaveForNextWeeks: (mealText: String, nutritionText: String) -> Unit,
    onReset: () -> Unit,
    onRecalculateNutritionWithGemini: (mealText: String) -> Unit
) {
    var mealText by remember(dialogUi.slotId, dialogUi.mealText) {
        mutableStateOf(dialogUi.mealText)
    }
    var nutritionText by remember(dialogUi.slotId, dialogUi.nutritionText) {
        mutableStateOf(dialogUi.nutritionText)
    }

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

                EditSlotActionBar(
                    isGeminiRecalculating = dialogUi.isGeminiRecalculating,
                    canActOnMeal = mealText.isNotBlank(),
                    onRecalculateNutritionWithGemini = {
                        onRecalculateNutritionWithGemini(mealText)
                    },
                    onSave = { onSave(mealText, nutritionText) },
                    onSaveForNextWeeks = { onSaveForNextWeeks(mealText, nutritionText) },
                    onReset = onReset,
                    onDismiss = onDismiss
                )

                dialogUi.geminiMessage
                    ?.takeIf { it.isNotBlank() }
                    ?.let { geminiMessage ->
                        Text(
                            text = geminiMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun EditSlotActionBar(
    isGeminiRecalculating: Boolean,
    canActOnMeal: Boolean,
    onRecalculateNutritionWithGemini: () -> Unit,
    onSave: () -> Unit,
    onSaveForNextWeeks: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onRecalculateNutritionWithGemini,
                enabled = !isGeminiRecalculating && canActOnMeal,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (isGeminiRecalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Calcola nutrienti con Gemini"
                    )
                }
                Text(
                    text = " AI",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            FilledTonalIconButton(
                onClick = onSave,
                enabled = canActOnMeal,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Salva sul weekly plan corrente"
                )
            }

            FilledTonalIconButton(
                onClick = onSaveForNextWeeks,
                enabled = canActOnMeal,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Salva anche per le prossime settimane"
                )
            }

            OutlinedIconButton(
                onClick = onReset
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Ripristina"
                )
            }

            OutlinedIconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi"
                )
            }
        }
    }
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
