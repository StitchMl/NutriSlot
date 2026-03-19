@file:Suppress("AssignedValueIsNeverRead")

package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slotTimeLabel

private val PreviewSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

private val TimeRailWidth = 84.dp
private val DayColumnWidth = 208.dp
private val DayHeaderHeight = 72.dp
private val PreviewRowMinHeight = 156.dp

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
    if (!uiState.hasEditableDraft && uiState.importedDraft == null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Controlla import") }
                )
            }
        ) { innerPadding ->
            EmptyPreviewContent(
                innerPadding = innerPadding,
                onBackClick = onBackClick
            )
        }
        return
    }

    var editingCellId by rememberSaveable { mutableStateOf<String?>(null) }

    val visibleDays = remember(uiState.selectedPreviewDay) {
        uiState.selectedPreviewDay?.let(::listOf) ?: WeekDay.orderedValues()
    }

    val filteredCells = remember(
        uiState.editableCells,
        visibleDays,
        uiState.showOnlyFilledSlots
    ) {
        uiState.editableCells.filter { cell ->
            cell.dayOfWeek in visibleDays &&
                    (!uiState.showOnlyFilledSlots || cell.mealText.isNotBlank())
        }
    }

    val cellsByDayAndSlot = remember(filteredCells, visibleDays) {
        visibleDays.associateWith { day ->
            PreviewSlotOrder.associateWith { slotType ->
                filteredCells.firstOrNull { cell ->
                    cell.dayOfWeek == day && cell.mealSlotType == slotType
                }
            }
        }
    }

    val editingCell = uiState.editableCells.firstOrNull { it.id == editingCellId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Controlla import") }
            )
        },
        bottomBar = {
            ImportPreviewBottomBar(
                uiState = uiState,
                onBackClick = onBackClick,
                onConfirmReviewClick = onConfirmReviewClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CompactPreviewControls(
                uiState = uiState,
                onTogglePreviewDay = onTogglePreviewDay,
                onToggleShowOnlyFilledSlots = onToggleShowOnlyFilledSlots
            )

            if (filteredCells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    EmptyFilteredStateCard()
                }
            } else {
                val horizontalScroll = rememberScrollState()
                val verticalScroll = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .horizontalScroll(horizontalScroll)
                        .verticalScroll(verticalScroll)
                ) {
                    Column {
                        PreviewCalendarHeaderRow(
                            visibleDays = visibleDays,
                            cellsByDayAndSlot = cellsByDayAndSlot
                        )

                        PreviewSlotOrder.forEach { slotType ->
                            PreviewCalendarBodyRow(
                                slotType = slotType,
                                visibleDays = visibleDays,
                                cellsByDayAndSlot = cellsByDayAndSlot,
                                onCellClick = { clicked -> editingCellId = clicked.id }
                            )
                        }
                    }
                }
            }
        }

        if (editingCell != null) {
            EditMealCellDialog(
                cell = editingCell,
                onDismiss = { editingCellId = null },
                onSave = { updatedText ->
                    onMealTextChange(editingCell.id, updatedText)
                    editingCellId = null
                },
                onClear = {
                    onClearCellClick(editingCell.id)
                    editingCellId = null
                }
            )
        }
    }
}

@Composable
private fun CompactPreviewControls(
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

@Composable
private fun PreviewCalendarHeaderRow(
    visibleDays: List<WeekDay>,
    cellsByDayAndSlot: Map<WeekDay, Map<MealSlotType, EditableImportedMealCellUi?>>
) {
    Row(
        modifier = Modifier.height(DayHeaderHeight)
    ) {
        Spacer(
            modifier = Modifier
                .width(TimeRailWidth)
                .height(DayHeaderHeight)
        )

        visibleDays.forEach { day ->
            val dayCells = cellsByDayAndSlot[day].orEmpty().values.filterNotNull()
            val filledCount = dayCells.count { it.mealText.isNotBlank() }

            Surface(
                modifier = Modifier
                    .width(DayColumnWidth)
                    .height(DayHeaderHeight)
                    .padding(start = 8.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = day.displayName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = day.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$filledCount/${PreviewSlotOrder.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCalendarBodyRow(
    slotType: MealSlotType,
    visibleDays: List<WeekDay>,
    cellsByDayAndSlot: Map<WeekDay, Map<MealSlotType, EditableImportedMealCellUi?>>,
    onCellClick: (EditableImportedMealCellUi) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PreviewRowMinHeight)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .width(TimeRailWidth)
                .heightIn(min = PreviewRowMinHeight),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = slotTimeLabel(slotType),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = slotType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        visibleDays.forEach { day ->
            val cell = cellsByDayAndSlot[day]?.get(slotType)

            EditableMealCellCard(
                modifier = Modifier
                    .width(DayColumnWidth)
                    .padding(start = 8.dp),
                cell = cell,
                slotType = slotType,
                onClick = {
                    if (cell != null) onCellClick(cell)
                }
            )
        }
    }
}

@Composable
private fun EditMealCellDialog(
    cell: EditableImportedMealCellUi,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var draftText by remember(cell.id) { mutableStateOf(cell.mealText) }

    LaunchedEffect(cell.id) {
        draftText = cell.mealText
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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

                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 8,
                    maxLines = 14,
                    placeholder = { Text("Inserisci o correggi il pasto") }
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

@Composable
private fun ImportPreviewBottomBar(
    uiState: ImportFileUiState,
    onBackClick: () -> Unit,
    onConfirmReviewClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onConfirmReviewClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(if (uiState.isLoading) "Salvataggio..." else "Conferma e salva")
            }

            FilledTonalButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Torna all'import")
            }
        }
    }
}