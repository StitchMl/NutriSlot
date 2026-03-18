package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi

@Composable
internal fun EditableMealCellCard(
    cell: EditableImportedMealCellUi,
    onMealTextChange: (cellId: String, newValue: String) -> Unit,
    onClearCellClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = cell.mealSlotType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            PreviewStatusBadge(
                text = recognitionLabel(
                    state = cell.originalRecognitionState,
                    wasManuallyEdited = cell.wasManuallyEdited
                ),
                containerColor = recognitionContainerColor(
                    state = cell.originalRecognitionState,
                    wasManuallyEdited = cell.wasManuallyEdited
                ),
                contentColor = recognitionContentColor(
                    state = cell.originalRecognitionState,
                    wasManuallyEdited = cell.wasManuallyEdited
                )
            )

            OutlinedTextField(
                value = cell.mealText,
                onValueChange = { newValue -> onMealTextChange(cell.id, newValue) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pasto") },
                minLines = 3
            )

            FilledTonalButton(
                onClick = onClearCellClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Svuota slot")
            }
        }
    }
}

private fun recognitionLabel(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): String {
    if (wasManuallyEdited) {
        return "Modificato manualmente"
    }

    return when (state) {
        CellRecognitionState.RECOGNIZED -> "Riconosciuto automaticamente"
        CellRecognitionState.SUSPECTED -> "Riconoscimento dubbio"
        CellRecognitionState.MISSING_DAY -> "Giorno mancante"
        CellRecognitionState.MISSING_MEAL_SLOT -> "Slot mancante"
        CellRecognitionState.EMPTY -> "Vuoto"
    }
}

@Composable
private fun recognitionContainerColor(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
) = when {
    wasManuallyEdited -> MaterialTheme.colorScheme.tertiaryContainer
    state == CellRecognitionState.RECOGNIZED -> MaterialTheme.colorScheme.primaryContainer
    state == CellRecognitionState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun recognitionContentColor(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
) = when {
    wasManuallyEdited -> MaterialTheme.colorScheme.onTertiaryContainer
    state == CellRecognitionState.RECOGNIZED -> MaterialTheme.colorScheme.onPrimaryContainer
    state == CellRecognitionState.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onErrorContainer
}