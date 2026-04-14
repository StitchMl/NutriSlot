package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.mealSemanticLabel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.parseMealSectionVisuals
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest

@Composable
fun EditSlotDialog(
    dialogUi: EditSlotDialogUi,
    onDismiss: () -> Unit,
    onSave: (EditSlotSaveRequest) -> Unit,
    onSaveForNextWeeks: (EditSlotSaveRequest) -> Unit,
    onReset: () -> Unit,
    onRecalculateNutritionWithGemini: (mealText: String) -> Unit
) {
    var mealText by remember(dialogUi.slotId, dialogUi.mealText) {
        mutableStateOf(dialogUi.mealText)
    }
    var nutritionText by remember(dialogUi.slotId, dialogUi.nutritionText) {
        mutableStateOf(dialogUi.nutritionText)
    }
    var selectedTargetKeys by remember(
        dialogUi.slotId,
        dialogUi.selectedConsumptionTargetCanonicalKeys
    ) {
        mutableStateOf(dialogUi.selectedConsumptionTargetCanonicalKeys.distinct())
    }

    val didUserEditConsumptionTargets = selectedTargetKeys.toSet() !=
        dialogUi.selectedConsumptionTargetCanonicalKeys.distinct().toSet()
    val isGeminiBusy = dialogUi.isGeminiRecalculating || dialogUi.isGeminiCatalogingTargets
    val parsedSections = remember(mealText) {
        parseMealSectionVisuals(mealText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            EditSlotHeroCard(
                dialogUi = dialogUi,
                selectedTargetCount = selectedTargetKeys.size,
                didUserEditConsumptionTargets = didUserEditConsumptionTargets
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (parsedSections.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Anteprima rapida",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                parsedSections.take(2).forEachIndexed { index, section ->
                                    if (index > 0) {
                                        EditDialogPill(
                                            text = "Alternativa ${index + 1}",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
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
                                                text = "${section.visualInfo.emoji} ${section.lines.firstOrNull() ?: mealSemanticLabel(
                                                    section.visualInfo.semanticKey
                                                )
                                                }",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            section.lines.drop(1).take(2).forEach { line ->
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
                        label = { Text("Pasto") },
                        minLines = 4,
                        supportingText = {
                            Text("Scrivi il pasto liberamente. La preview ripulisce nutrienti e kcal.")
                        }
                    )

                    OutlinedTextField(
                        value = nutritionText,
                        onValueChange = { nutritionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nutrienti opzionali") },
                        minLines = 1,
                        supportingText = {
                            Text("Restano nel box ma non finiscono nella lista spesa.")
                        }
                    )

                    if (dialogUi.availableConsumptionTargets.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Target di consumo",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = targetSelectionHint(
                                                dialogUi = dialogUi,
                                                didUserEditConsumptionTargets = didUserEditConsumptionTargets
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    EditDialogPill(
                                        text = if (selectedTargetKeys.isEmpty()) {
                                            "0 scelti"
                                        } else {
                                            "${selectedTargetKeys.size} scelti"
                                        },
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dialogUi.availableConsumptionTargets.forEach { target ->
                                        val isSelected = target.canonicalKey in selectedTargetKeys
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedTargetKeys = if (isSelected) {
                                                    selectedTargetKeys - target.canonicalKey
                                                } else {
                                                    selectedTargetKeys + target.canonicalKey
                                                }
                                            },
                                            enabled = !isGeminiBusy,
                                            label = {
                                                Text("${targetEmojiForCanonicalKey(target.canonicalKey)} ${target.title}")
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = targetChipAccent(target.canonicalKey),
                                                selectedLabelColor = Color(0xFF2B2118),
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                                                labelColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    dialogUi.geminiMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.let { geminiMessage ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                            ) {
                                Text(
                                    text = geminiMessage,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }

                EditSlotActionBar(
                    isGeminiRecalculating = dialogUi.isGeminiRecalculating,
                    isGeminiCatalogingTargets = dialogUi.isGeminiCatalogingTargets,
                    canActOnMeal = mealText.isNotBlank(),
                    onRecalculateNutritionWithGemini = {
                        onRecalculateNutritionWithGemini(mealText)
                    },
                    onSave = {
                        onSave(
                            buildEditSlotSaveRequest(
                                mealText = mealText,
                                nutritionText = nutritionText,
                                selectedTargetKeys = selectedTargetKeys,
                                didUserEditConsumptionTargets = didUserEditConsumptionTargets
                            )
                        )
                    },
                    onSaveForNextWeeks = {
                        onSaveForNextWeeks(
                            buildEditSlotSaveRequest(
                                mealText = mealText,
                                nutritionText = nutritionText,
                                selectedTargetKeys = selectedTargetKeys,
                                didUserEditConsumptionTargets = didUserEditConsumptionTargets
                            )
                        )
                    },
                    onReset = onReset,
                    onDismiss = onDismiss
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun EditSlotActionBar(
    isGeminiRecalculating: Boolean,
    isGeminiCatalogingTargets: Boolean,
    canActOnMeal: Boolean,
    onRecalculateNutritionWithGemini: () -> Unit,
    onSave: () -> Unit,
    onSaveForNextWeeks: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val isGeminiBusy = isGeminiRecalculating || isGeminiCatalogingTargets

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onSave,
                enabled = canActOnMeal && !isGeminiBusy,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Salva",
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onSaveForNextWeeks,
                enabled = canActOnMeal && !isGeminiBusy,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Salva per il futuro",
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onRecalculateNutritionWithGemini,
                enabled = !isGeminiBusy && canActOnMeal,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (isGeminiBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Calcola con Gemini",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = onReset,
                enabled = !isGeminiBusy,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Ripristina",
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledIconButton(
                onClick = onDismiss,
                enabled = !isGeminiBusy,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EditSlotHeroCard(
    dialogUi: EditSlotDialogUi,
    selectedTargetCount: Int,
    didUserEditConsumptionTargets: Boolean
) {
    val gradientColors = when {
        didUserEditConsumptionTargets || dialogUi.consumptionTargetSource == MealConsumptionTargetSource.MANUAL -> {
            listOf(Color(0xFFF7C58B), Color(0xFFF3A18F), MaterialTheme.colorScheme.primaryContainer)
        }

        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.GEMINI -> {
            listOf(Color(0xFFA9E2D0), Color(0xFFB7D7FF), MaterialTheme.colorScheme.secondaryContainer)
        }

        else -> {
            listOf(Color(0xFFF7E7CE), Color(0xFFF3D9B1), Color(0xFFFFE6B4))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = MaterialTheme.shapes.large
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Modifica rapida",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4A382C)
                    )
                    Text(
                        text = "${dialogUi.dayLabel} | ${dialogUi.mealSlotLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C211B)
                    )
                    Text(
                        text = slotTimeLabelFromLabel(dialogUi.mealSlotLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5E4A3E)
                    )
                }

                EditDialogPill(
                    text = headerSourceLabel(
                        source = dialogUi.consumptionTargetSource,
                        didUserEditConsumptionTargets = didUserEditConsumptionTargets
                    ),
                    containerColor = Color.White.copy(alpha = 0.78f),
                    contentColor = Color(0xFF2C211B)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditDialogPill(
                    text = if (selectedTargetCount == 0) "Nessun target" else "$selectedTargetCount target",
                    containerColor = Color.White.copy(alpha = 0.78f),
                    contentColor = Color(0xFF2C211B)
                )
                EditDialogPill(
                    text = if (dialogUi.nutritionText.isBlank()) "Nutrienti opzionali" else "Nutrienti presenti",
                    containerColor = Color.White.copy(alpha = 0.66f),
                    contentColor = Color(0xFF5C473A)
                )
            }
        }
    }
}

@Composable
private fun EditDialogPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildEditSlotSaveRequest(
    mealText: String,
    nutritionText: String,
    selectedTargetKeys: List<String>,
    didUserEditConsumptionTargets: Boolean
): EditSlotSaveRequest {
    return EditSlotSaveRequest(
        mealText = mealText,
        nutritionText = nutritionText,
        selectedConsumptionTargetCanonicalKeys = selectedTargetKeys.distinct(),
        didUserEditConsumptionTargets = didUserEditConsumptionTargets
    )
}

private fun targetSelectionHint(
    dialogUi: EditSlotDialogUi,
    didUserEditConsumptionTargets: Boolean
): String {
    return when {
        didUserEditConsumptionTargets -> "Hai scelto tu cosa tracciare."
        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.MANUAL -> {
            "Questa selezione resta fissa finche non la cambi."
        }

        dialogUi.consumptionTargetSource == MealConsumptionTargetSource.GEMINI -> {
            "Puoi confermare o correggere i target riconosciuti."
        }

        else -> {
            "Se cambi il pasto senza toccare i chip, i target si aggiornano da soli."
        }
    }
}

private fun headerSourceLabel(
    source: MealConsumptionTargetSource?,
    didUserEditConsumptionTargets: Boolean
): String {
    return when {
        didUserEditConsumptionTargets -> "Manuale"
        source == MealConsumptionTargetSource.MANUAL -> "Manuale"
        source == MealConsumptionTargetSource.GEMINI -> "Gemini"
        else -> "Auto"
    }
}

private fun targetEmojiForCanonicalKey(
    canonicalKey: String
): String {
    return when (canonicalKey) {
        "acqua" -> "💧"
        "frutta e verdura" -> "🥦"
        "caffe e the" -> "☕"
        "carne bianca" -> "🍗"
        "carne rossa" -> "🥩"
        "affettati" -> "🥓"
        "uova" -> "🥚"
        "formaggi" -> "🧀"
        "patate" -> "🥔"
        "piatto unico" -> "🍲"
        "pesce" -> "🐟"
        "legumi" -> "🫘"
        else -> "✨"
    }
}

private fun targetChipAccent(
    canonicalKey: String
): Color {
    return when (canonicalKey) {
        "acqua" -> Color(0xFFBEE7FF)
        "frutta e verdura" -> Color(0xFFD0EAA9)
        "caffe e the" -> Color(0xFFF6D4A7)
        "carne bianca" -> Color(0xFFFFD8AE)
        "carne rossa" -> Color(0xFFF4B4AD)
        "affettati" -> Color(0xFFFFC9BF)
        "uova" -> Color(0xFFFFE7A3)
        "formaggi" -> Color(0xFFFFE3A9)
        "patate" -> Color(0xFFF2D3A7)
        "piatto unico" -> Color(0xFFDCCAFF)
        "pesce" -> Color(0xFFBDD9FF)
        "legumi" -> Color(0xFFD9C7FF)
        else -> Color(0xFFF0D9CC)
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
