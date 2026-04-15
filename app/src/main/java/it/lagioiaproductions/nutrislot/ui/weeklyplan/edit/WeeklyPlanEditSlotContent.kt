package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.ParsedMealSectionUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.mealSemanticLabel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi

/**
 * Renders the body of the slot editing dialog, keeping the dialog shell free from UI details.
 */
@Composable
internal fun EditSlotDialogContent(
    dialogUi: EditSlotDialogUi,
    mealText: String,
    onMealTextChange: (String) -> Unit,
    nutritionText: String,
    onNutritionTextChange: (String) -> Unit,
    selectedTargetKeys: List<String>,
    onToggleTargetKey: (String) -> Unit,
    didUserEditConsumptionTargets: Boolean,
    isGeminiBusy: Boolean,
    parsedSections: List<ParsedMealSectionUi>,
    onRecalculateNutritionWithGemini: () -> Unit,
    onSave: () -> Unit,
    onSaveForNextWeeks: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditSlotPreviewSection(parsedSections = parsedSections)
        EditSlotTextFieldsSection(
            mealText = mealText,
            onMealTextChange = onMealTextChange,
            nutritionText = nutritionText,
            onNutritionTextChange = onNutritionTextChange
        )
        EditSlotConsumptionTargetsSection(
            dialogUi = dialogUi,
            selectedTargetKeys = selectedTargetKeys,
            onToggleTargetKey = onToggleTargetKey,
            didUserEditConsumptionTargets = didUserEditConsumptionTargets,
            isGeminiBusy = isGeminiBusy
        )
        EditSlotActionBar(
            isGeminiRecalculating = dialogUi.isGeminiRecalculating,
            isGeminiCatalogingTargets = dialogUi.isGeminiCatalogingTargets,
            canActOnMeal = mealText.isNotBlank(),
            onRecalculateNutritionWithGemini = onRecalculateNutritionWithGemini,
            onSave = onSave,
            onSaveForNextWeeks = onSaveForNextWeeks,
            onReset = onReset,
            onDismiss = onDismiss,
            canReset = dialogUi.canResetToOriginal
        )
        EditSlotGeminiMessage(message = dialogUi.geminiMessage)
    }
}

/**
 * Shows a compact preview of the parsed meal alternatives to make free-form edits easier to read.
 */
@Composable
private fun EditSlotPreviewSection(parsedSections: List<ParsedMealSectionUi>) {
    if (parsedSections.isEmpty()) return

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
                            text = buildPreviewHeadline(section),
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

/**
 * Keeps the editable text inputs grouped together so the parent content stays scan-friendly.
 */
@Composable
private fun EditSlotTextFieldsSection(
    mealText: String,
    onMealTextChange: (String) -> Unit,
    nutritionText: String,
    onNutritionTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = mealText,
        onValueChange = onMealTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Pasto") },
        minLines = 6,
        supportingText = {
            Text("Scrivi il pasto liberamente. La preview ripulisce nutrienti e kcal.")
        }
    )

    OutlinedTextField(
        value = nutritionText,
        onValueChange = onNutritionTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Nutrienti opzionali") },
        minLines = 2,
        supportingText = {
            Text("Restano nel box ma non finiscono nella lista spesa.")
        }
    )
}

/**
 * Renders the manual target selection area and keeps chip behavior local to this section.
 */
@Composable
private fun EditSlotConsumptionTargetsSection(
    dialogUi: EditSlotDialogUi,
    selectedTargetKeys: List<String>,
    onToggleTargetKey: (String) -> Unit,
    didUserEditConsumptionTargets: Boolean,
    isGeminiBusy: Boolean
) {
    if (dialogUi.availableConsumptionTargets.isEmpty()) return

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                        onClick = { onToggleTargetKey(target.canonicalKey) },
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

/**
 * Surfaces Gemini feedback without mixing messaging logic into the primary form blocks.
 */
@Composable
private fun EditSlotGeminiMessage(message: String?) {
    message
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

/**
 * Generates the preview title by preferring the first parsed line and falling back to the semantic label.
 */
private fun buildPreviewHeadline(section: ParsedMealSectionUi): String {
    val primaryLine = section.lines.firstOrNull() ?: mealSemanticLabel(section.visualInfo.semanticKey)
    return "${section.visualInfo.emoji} $primaryLine"
}
