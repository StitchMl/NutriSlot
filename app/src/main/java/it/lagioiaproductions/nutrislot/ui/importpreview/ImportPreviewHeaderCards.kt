package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Controlla e correggi la settimana",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            uiState.selectedFileName?.let { fileName ->
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.infoMessage?.let { info ->
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewStatusBadge(
                    text = "Parsing: $statusText",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                PreviewStatusBadge(
                    text = "Slot pieni: ${uiState.populatedEditableCellsCount}/${uiState.editableCells.size}"
                )

                PreviewStatusBadge(
                    text = "Opzioni extra: ${uiState.additionalOptionsCount}"
                )

                PreviewStatusBadge(
                    text = "Regole: ${uiState.mealRulesCount}"
                )

                PreviewStatusBadge(
                    text = "Modifiche: ${uiState.editedCellsCount}",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                PreviewStatusBadge(
                    text = "Warning: ${uiState.warnings.size}",
                    containerColor = if (uiState.warnings.isNotEmpty()) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (uiState.warnings.isNotEmpty()) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
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
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Riduci il numero di celle visibili prima della revisione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}