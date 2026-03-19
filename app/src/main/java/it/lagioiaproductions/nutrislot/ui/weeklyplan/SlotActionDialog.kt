@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectExtraCatalogOption: (optionId: String) -> Unit
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
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WeeklyStatusBadge(
                    text = "Stato: ${slotStatusLabel(dialogUi.targetDisplayState)}",
                    containerColor = slotStatusContainerColor(dialogUi.targetDisplayState),
                    contentColor = slotStatusContentColor(dialogUi.targetDisplayState)
                )

                dialogUi.mealRuleSummary?.let { ruleSummary ->
                    DialogInfoBlock(
                        title = "Composizione attesa",
                        body = ruleSummary
                    )
                }

                val targetSections = parseMealSections(dialogUi.currentDisplayedMealText)

                if (targetSections.isEmpty()) {
                    Text(
                        text = "Questo slot non ha un pasto disponibile in questo momento.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    DialogSectionTitle("Pasto attualmente assegnato")
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
                                "Segna come consumato"
                            }
                        )
                    }
                }

                if (dialogUi.replacementOptions.isNotEmpty()) {
                    DialogSectionTitle("Sostituisci con un pasto già pianificato")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dialogUi.replacementOptions.forEach { replacement ->
                            ReplacementOptionCard(
                                option = replacement,
                                enabled = !isApplying,
                                onClick = {
                                    onConsumeReplacement(replacement.sourceSlotId)
                                }
                            )
                        }
                    }
                }

                if (dialogUi.extraCatalogOptions.isNotEmpty()) {
                    DialogSectionTitle("Opzioni extra dal PDF")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dialogUi.extraCatalogOptions.take(4).forEach { option ->
                            ExtraCatalogOptionCard(
                                option = option,
                                enabled = !isApplying,
                                onClick = {
                                    onSelectExtraCatalogOption(option.optionId)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isApplying
            ) {
                Text("Chiudi")
            }
        },
        dismissButton = null
    )
}

@Composable
private fun DialogSectionTitle(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun DialogInfoBlock(
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ReplacementOptionCard(
    option: ReplacementMealOptionUi,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeeklyStatusBadge(
                    text = "${option.sourceDayLabel} • ${option.sourceMealSlotLabel}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = option.mealText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExtraCatalogOptionCard(
    option: ExtraCatalogMealOptionUi,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            option.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = option.mealText,
                style = MaterialTheme.typography.bodyMedium
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyStatusBadge(
                    text = option.sourceLabel,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                option.pageNumber?.let { page ->
                    WeeklyStatusBadge(
                        text = "Pag. $page",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MealTextBlock(
    sections: List<List<String>>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    SectionSeparatorBadge()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    section.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSeparatorBadge() {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "Dettaglio",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}