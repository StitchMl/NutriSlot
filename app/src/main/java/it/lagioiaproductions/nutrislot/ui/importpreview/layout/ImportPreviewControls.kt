package it.lagioiaproductions.nutrislot.ui.importpreview.layout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.state.ImportFileUiState

/** Compact control bar used to filter the preview by day and slot occupancy. */
@Composable
internal fun CompactPreviewControls(
    uiState: ImportFileUiState,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = uiState.selectedFileName ?: "Anteprima import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = if (uiState.isManualDraft) {
                    "Compila un giorno alla volta. Tocca uno slot per inserire il pasto e usa i chip per cambiare giorno."
                } else {
                    "Controlla i pasti trovati dal PDF, correggi gli slot da rivedere e poi salva."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedPreviewDay == null,
                    onClick = { onTogglePreviewDay(null) },
                    label = { Text("Tutti") }
                )

                WeekDay.orderedValues().forEach { day ->
                    FilterChip(
                        selected = uiState.selectedPreviewDay == day,
                        onClick = { onTogglePreviewDay(day) },
                        label = { Text(day.displayName.take(3)) }
                    )
                }
            }

            FilterChip(
                selected = uiState.showOnlyFilledSlots,
                onClick = onToggleShowOnlyFilledSlots,
                label = {
                    Text(if (uiState.showOnlyFilledSlots) "Solo compilati" else "Mostra anche vuoti")
                }
            )
        }
    }
}
