package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    uiState: ImportFileUiState,
    onMealTextChange: (cellId: String, newValue: String) -> Unit,
    onClearCellClick: (cellId: String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmReviewClick: () -> Unit,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Anteprima import")
                }
            )
        }
    ) { innerPadding ->
        if (!uiState.hasEditableDraft) {
            EmptyPreviewContent(
                innerPadding = innerPadding,
                onBackClick = onBackClick
            )
        } else {
            val filteredCellsByDay = uiState.filteredEditableCells.groupBy { it.dayOfWeek }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .safeDrawingPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PreviewHeroCard(uiState = uiState)
                }

                item {
                    PreviewFiltersCard(
                        uiState = uiState,
                        onTogglePreviewDay = onTogglePreviewDay,
                        onToggleShowOnlyFilledSlots = onToggleShowOnlyFilledSlots
                    )
                }

                if (uiState.warnings.isNotEmpty()) {
                    item {
                        ImportWarningsCard(
                            warnings = uiState.warnings.map { it.message }
                        )
                    }
                }

                if (uiState.filteredEditableCells.isEmpty()) {
                    item {
                        EmptyFilteredStateCard()
                    }
                } else {
                    val visibleDays = WeekDay.orderedValues()
                        .filter { day -> filteredCellsByDay[day].orEmpty().isNotEmpty() }

                    visibleDays.forEach { day ->
                        val dayCells = filteredCellsByDay[day].orEmpty()

                        item(key = "header_${day.name}") {
                            DayHeaderCard(dayDisplayName = day.displayName)
                        }

                        items(
                            items = dayCells,
                            key = { it.id }
                        ) { cell ->
                            EditableMealCellCard(
                                cell = cell,
                                onMealTextChange = onMealTextChange,
                                onClearCellClick = { onClearCellClick(cell.id) }
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Button(
                        onClick = onConfirmReviewClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Conferma revisione")
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Torna all'import")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPreviewContent(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nessuna anteprima disponibile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Prima devi selezionare un PDF nella schermata di import.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        FilledTonalButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Indietro")
        }
    }
}

@Composable
private fun PreviewHeroCard(
    uiState: ImportFileUiState
) {
    val statusText = when (uiState.importStatus) {
        ImportStatus.SUCCESS -> "Successo"
        ImportStatus.PARTIAL -> "Parziale"
        ImportStatus.UNSUPPORTED -> "Non supportato"
        ImportStatus.FAILED -> "Fallito"
        null -> "N/D"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
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

            StatusBadge(text = "Stato parsing: $statusText")
            StatusBadge(text = "Slot con contenuto: ${uiState.populatedEditableCellsCount} / ${uiState.editableCells.size}")
            StatusBadge(text = "Celle modificate: ${uiState.editedCellsCount}")
            StatusBadge(text = "Warning: ${uiState.warnings.size}")
        }
    }
}

@Composable
private fun PreviewFiltersCard(
    uiState: ImportFileUiState,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filtri rapidi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
private fun ImportWarningsCard(
    warnings: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Warning del parser",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            warnings.forEach { warning ->
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmptyFilteredStateCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nessuna cella visibile con i filtri attuali",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Prova a cambiare giorno oppure a disattivare il filtro sugli slot valorizzati.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DayHeaderCard(
    dayDisplayName: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = dayDisplayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EditableMealCellCard(
    cell: EditableImportedMealCellUi,
    onMealTextChange: (cellId: String, newValue: String) -> Unit,
    onClearCellClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = cell.mealSlotType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            StatusBadge(
                text = recognitionLabel(
                    state = cell.originalRecognitionState,
                    wasManuallyEdited = cell.wasManuallyEdited
                )
            )

            OutlinedTextField(
                value = cell.mealText,
                onValueChange = { newValue ->
                    onMealTextChange(cell.id, newValue)
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Pasto")
                },
                minLines = 2
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

@Composable
private fun StatusBadge(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
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