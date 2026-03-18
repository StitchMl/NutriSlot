package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState

@Composable
internal fun PreviewHeroCard(
    uiState: ImportFileUiState
) {
    val statusText = when (uiState.importStatus) {
        ImportStatus.SUCCESS -> "Successo"
        ImportStatus.PARTIAL -> "Parziale"
        ImportStatus.UNSUPPORTED -> "Non supportato"
        ImportStatus.FAILED -> "Fallito"
        null -> "N/D"
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Controlla e correggi la settimana",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            uiState.selectedFileName?.let { fileName ->
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.infoMessage?.let { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            PreviewStatusBadge(text = "Stato parsing: $statusText")
            PreviewStatusBadge(text = "Slot con contenuto: ${uiState.populatedEditableCellsCount} / ${uiState.editableCells.size}")
            PreviewStatusBadge(text = "Opzioni extra catturate: ${uiState.additionalOptionsCount}")
            PreviewStatusBadge(text = "Regole nutrizionali catturate: ${uiState.mealRulesCount}")
            PreviewStatusBadge(text = "Celle modificate: ${uiState.editedCellsCount}")
            PreviewStatusBadge(text = "Warning: ${uiState.warnings.size}")
        }
    }
}

@Composable
internal fun PreviewFiltersCard(
    uiState: ImportFileUiState,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filtri rapidi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedPreviewDay == null,
                        onClick = { onTogglePreviewDay(null) },
                        label = { Text("Tutti") }
                    )
                }

                items(WeekDay.orderedValues()) { day ->
                    FilterChip(
                        selected = uiState.selectedPreviewDay == day,
                        onClick = { onTogglePreviewDay(day) },
                        label = { Text(day.displayName) }
                    )
                }
            }

            FilterChip(
                selected = uiState.showOnlyFilledSlots,
                onClick = onToggleShowOnlyFilledSlots,
                label = {
                    Text("Mostra solo slot con contenuto")
                }
            )
        }
    }
}

@Composable
internal fun DayHeaderCard(
    dayDisplayName: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dayDisplayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun PreviewStatusBadge(
    text: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}