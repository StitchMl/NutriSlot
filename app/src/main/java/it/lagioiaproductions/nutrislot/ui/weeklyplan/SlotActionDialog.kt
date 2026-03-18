package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SlotActionDialog(
    dialogUi: SlotActionDialogUi,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isApplying) {
                onDismiss()
            }
        },
        title = {
            Text("${dialogUi.targetDayLabel} • ${dialogUi.targetMealSlotLabel}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stato attuale: ${slotStatusLabel(dialogUi.targetDisplayState)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                val targetSections = parseMealSections(dialogUi.currentDisplayedMealText)

                if (targetSections.isEmpty()) {
                    Text(
                        text = "Questo slot non ha un pasto disponibile in questo momento.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Pasto attualmente assegnato",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    MealTextBlock(sections = targetSections)
                }

                if (
                    dialogUi.reassignedFromDayLabel != null &&
                    dialogUi.reassignedFromMealSlotLabel != null &&
                    !dialogUi.isTargetActuallyCompletedThisWeek
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Questo slot contiene il pasto originario di ${dialogUi.reassignedFromDayLabel} • ${dialogUi.reassignedFromMealSlotLabel}.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (dialogUi.canConsumeAsPlanned) {
                    Button(
                        onClick = onConsumeAsPlanned,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApplying
                    ) {
                        Text(
                            if (isApplying) {
                                "Aggiornamento in corso..."
                            } else {
                                "Segna come completato"
                            }
                        )
                    }
                }

                if (!dialogUi.isTargetActuallyCompletedThisWeek) {
                    Text(
                        text = "Usa un pasto compatibile da un altro slot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    if (dialogUi.replacementOptions.isEmpty()) {
                        Text(
                            text = "Non ci sono altri pasti compatibili e disponibili da assegnare a questo slot.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        dialogUi.replacementOptions.forEach { option ->
                            ReplacementOptionButton(
                                option = option,
                                targetDayLabel = dialogUi.targetDayLabel,
                                targetMealSlotLabel = dialogUi.targetMealSlotLabel,
                                enabled = !isApplying,
                                onClick = { onConsumeReplacement(option.sourceSlotId) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isApplying
            ) {
                Text("Chiudi")
            }
        }
    )
}

@Composable
private fun ReplacementOptionButton(
    option: ReplacementMealOptionUi,
    targetDayLabel: String,
    targetMealSlotLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val sections = parseMealSections(option.mealText)
    val flatLines = sections.flatten()
    val previewLines = flatLines.take(3)
    val hiddenLinesCount = (flatLines.size - previewLines.size).coerceAtLeast(0)
    val extraSectionsCount = (sections.size - 1).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeeklyStatusBadge(
                    text = "Da ${option.sourceDayLabel}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                WeeklyStatusBadge(
                    text = option.sourceMealSlotLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Applica a $targetDayLabel • $targetMealSlotLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    previewLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (extraSectionsCount > 0) {
                        Text(
                            text = "Include anche $extraSectionsCount blocchi aggiuntivi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (hiddenLinesCount > 0) {
                        Text(
                            text = "+ $hiddenLinesCount dettagli",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                text = if (enabled) "Tocca per usare questo pasto" else "Operazione in corso...",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}