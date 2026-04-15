package it.lagioiaproductions.nutrislot.ui.importpreview.layout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.importfile.state.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.state.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.importpreview.components.EmptyFilteredStateCard
import it.lagioiaproductions.nutrislot.ui.importpreview.state.ImportPreviewGridState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

/** Top-level layout for the import preview grid and its auxiliary controls. */
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

        uiState.errorMessage?.let { errorMessage ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Attenzione",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (gridState.displayedCells.isEmpty()) {
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
                        filledCountByDay = gridState.filledCountByDay
                    )

                    gridState.visibleSlotTypes.forEach { slotType ->
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
