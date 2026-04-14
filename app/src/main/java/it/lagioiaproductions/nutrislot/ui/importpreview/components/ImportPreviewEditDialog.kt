package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parseMealSectionVisuals
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slotTimeLabel

@Composable
internal fun EditMealCellDialog(
    cell: EditableImportedMealCellUi,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var draftText by remember(cell.id) { mutableStateOf(cell.mealText) }

    LaunchedEffect(cell.id) {
        draftText = cell.mealText
    }

    val parsedSections = remember(draftText) {
        parseMealSectionVisuals(stripNutritionForPreview(draftText))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${cell.dayOfWeek.displayName} • ${cell.mealSlotType.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = slotTimeLabel(cell.mealSlotType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = recognitionBadgeContainer(
                        state = cell.originalRecognitionState,
                        wasManuallyEdited = cell.wasManuallyEdited
                    )
                ) {
                    Text(
                        text = recognitionBadgeLabel(
                            state = cell.originalRecognitionState,
                            wasManuallyEdited = cell.wasManuallyEdited
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = recognitionBadgeContent(
                            state = cell.originalRecognitionState,
                            wasManuallyEdited = cell.wasManuallyEdited
                        )
                    )
                }

                if (parsedSections.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
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

                            parsedSections.take(3).forEach { section ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${section.visualInfo.emoji} ${section.lines.firstOrNull() ?: previewSemanticLabel(section.visualInfo.semanticKey)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        section.lines.drop(1).take(2).forEach { line ->
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 8,
                    maxLines = 14,
                    placeholder = { Text("Inserisci o correggi il pasto") },
                    supportingText = {
                        Text(
                            text = when (cell.originalRecognitionState) {
                                CellRecognitionState.SUSPECTED ->
                                    "Cella dubbia: controlla bene testo, alternative e quantità."
                                CellRecognitionState.MISSING_DAY ->
                                    "Il giorno originale non era stato riconosciuto correttamente."
                                CellRecognitionState.MISSING_MEAL_SLOT ->
                                    "Lo slot originale non era stato riconosciuto correttamente."
                                CellRecognitionState.EMPTY ->
                                    "Slot vuoto."
                                CellRecognitionState.RECOGNIZED ->
                                    "Parsing riconosciuto."
                            }
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onClear) {
                        Text("Svuota")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Annulla")
                        }
                        Button(onClick = { onSave(draftText) }) {
                            Text("Salva")
                        }
                    }
                }
            }
        }
    }
}

private fun recognitionBadgeLabel(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): String {
    if (wasManuallyEdited) return "Modificato"

    return when (state) {
        CellRecognitionState.RECOGNIZED -> "Riconosciuto"
        CellRecognitionState.SUSPECTED -> "Dubbio"
        CellRecognitionState.MISSING_DAY -> "Giorno mancante"
        CellRecognitionState.MISSING_MEAL_SLOT -> "Slot mancante"
        CellRecognitionState.EMPTY -> "Vuoto"
    }
}

@Composable
private fun recognitionBadgeContainer(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): Color {
    return when {
        wasManuallyEdited -> MaterialTheme.colorScheme.secondaryContainer
        state == CellRecognitionState.RECOGNIZED -> MaterialTheme.colorScheme.primaryContainer
        state == CellRecognitionState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.errorContainer
    }
}

@Composable
private fun recognitionBadgeContent(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): Color {
    return when {
        wasManuallyEdited -> MaterialTheme.colorScheme.onSecondaryContainer
        state == CellRecognitionState.RECOGNIZED -> MaterialTheme.colorScheme.onPrimaryContainer
        state == CellRecognitionState.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
}

private fun previewSemanticLabel(semanticKey: String): String {
    return when (semanticKey) {
        "panino" -> "Panino"
        "piadina" -> "Piadina"
        "frisella" -> "Frisella"
        "insalata" -> "Insalata"
        "cereale_primo" -> "Primo"
        "pancake" -> "Pancake"
        "latticino" -> "Latticino"
        "carne" -> "Carne"
        "pesce" -> "Pesce"
        "uova" -> "Uova"
        "pane" -> "Pane"
        "colazione_secca" -> "Colazione"
        "banana" -> "Banana"
        "mela" -> "Mela"
        "pera" -> "Pera"
        "frutta" -> "Frutta"
        "frutta_secca" -> "Frutta secca"
        "avocado" -> "Avocado"
        "formaggio" -> "Formaggio"
        "pomodoro" -> "Pomodoro"
        "carota" -> "Carota"
        "verdura" -> "Verdura"
        "olio" -> "Olio"
        "dolce_spalmabile" -> "Dolce"
        "caffe" -> "Caffè"
        else -> "Pasto"
    }
}

private fun stripNutritionForPreview(text: String): String {
    val normalized = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val lines = normalized.lines()
    if (lines.size > 1) {
        val keptLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                keptLines += line
                continue
            }
            if (isNutritionLineLocal(trimmed)) break
            keptLines += line
        }

        val joined = keptLines.joinToString(separator = "\n").trim()
        if (joined.isNotBlank()) return joined
    }

    return normalized
        .replace(Regex("(?is)\\bNutrienti\\s*:.*$"), "")
        .replace(
            Regex("(?is)\\bTot\\.?\\s*(?:kcal|g\\s+proteine|g\\s+carboidrati|g\\s+fibre|g\\s+grassi|g\\s+lipidi)\\b.*$"),
            ""
        )
        .trim()
}

private fun isNutritionLineLocal(line: String): Boolean {
    val normalized = line
        .lowercase()
        .replace("’", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    return normalized.startsWith("nutrienti") ||
            normalized.startsWith("tot kcal") ||
            normalized.startsWith("tot. kcal") ||
            normalized.startsWith("tot g proteine") ||
            normalized.startsWith("tot. g proteine") ||
            normalized.startsWith("tot g carboidrati") ||
            normalized.startsWith("tot. g carboidrati") ||
            normalized.startsWith("tot g fibre") ||
            normalized.startsWith("tot. g fibre") ||
            normalized.startsWith("tot g grassi") ||
            normalized.startsWith("tot. g grassi") ||
            normalized.startsWith("tot g lipidi") ||
            normalized.startsWith("tot. g lipidi")
}