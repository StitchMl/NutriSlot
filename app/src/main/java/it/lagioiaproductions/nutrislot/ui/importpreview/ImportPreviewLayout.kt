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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slotTimeLabel

internal val TimeRailWidth = 84.dp
internal val DayColumnWidth = 208.dp
internal val DayHeaderHeight = 72.dp
internal val PreviewRowMinHeight = 156.dp

@Composable
internal fun ImportPreviewContent(
    modifier: Modifier = Modifier,
    uiState: ImportFileUiState,
    gridState: ImportPreviewGridState,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit,
    onCellClick: (EditableImportedMealCellUi) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CompactPreviewControls(
            uiState = uiState,
            onTogglePreviewDay = onTogglePreviewDay,
            onToggleShowOnlyFilledSlots = onToggleShowOnlyFilledSlots
        )

        if (gridState.filteredCells.isEmpty()) {
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
                        visibleDays = gridState.visibleDays,
                        cellsByDayAndSlot = gridState.cellsByDayAndSlot
                    )

                    PreviewSlotOrder.forEach { slotType ->
                        PreviewCalendarBodyRow(
                            slotType = slotType,
                            visibleDays = gridState.visibleDays,
                            cellsByDayAndSlot = gridState.cellsByDayAndSlot,
                            onCellClick = onCellClick
                        )
                    }
                }
            }
        }
    }
}

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
internal fun PreviewCalendarHeaderRow(
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
internal fun PreviewCalendarBodyRow(
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
internal fun ImportPreviewBottomBar(
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