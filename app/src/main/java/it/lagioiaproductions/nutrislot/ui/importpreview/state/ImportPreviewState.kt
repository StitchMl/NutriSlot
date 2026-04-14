package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState

internal val PreviewSlotOrder = listOf(
    MealSlotType.BREAKFAST,
    MealSlotType.MORNING_SNACK,
    MealSlotType.LUNCH,
    MealSlotType.AFTERNOON_SNACK,
    MealSlotType.DINNER
)

internal data class ImportPreviewGridState(
    val visibleDays: List<WeekDay>,
    val visibleSlotTypes: List<MealSlotType>,
    val displayedCells: List<EditableImportedMealCellUi>,
    val cellsByDayAndSlot: Map<WeekDay, Map<MealSlotType, EditableImportedMealCellUi?>>,
    val filledCountByDay: Map<WeekDay, Int>,
    val editingCell: EditableImportedMealCellUi?
)

internal data class ImportPreviewVisibility(
    val visibleDays: List<WeekDay>,
    val visibleSlotTypes: List<MealSlotType>,
    val availableCells: List<EditableImportedMealCellUi>,
    val displayedCells: List<EditableImportedMealCellUi>
)

@Composable
internal fun rememberImportPreviewGridState(
    uiState: ImportFileUiState,
    editingCellId: String?
): ImportPreviewGridState {
    val visibility = remember(
        uiState.editableCells,
        uiState.selectedPreviewDay,
        uiState.showOnlyFilledSlots
    ) {
        resolveImportPreviewVisibility(uiState)
    }

    val cellsByDayAndSlot = remember(
        visibility.displayedCells,
        visibility.visibleDays,
        visibility.visibleSlotTypes
    ) {
        visibility.visibleDays.associateWith { day ->
            visibility.visibleSlotTypes.associateWith { slotType ->
                visibility.displayedCells.firstOrNull { cell ->
                    cell.dayOfWeek == day && cell.mealSlotType == slotType
                }
            }
        }
    }

    val filledCountByDay = remember(visibility.availableCells, visibility.visibleDays) {
        visibility.visibleDays.associateWith { day ->
            visibility.availableCells.count { cell ->
                cell.dayOfWeek == day && cell.mealText.isNotBlank()
            }
        }
    }

    val editingCell = remember(uiState.editableCells, editingCellId) {
        uiState.editableCells.firstOrNull { it.id == editingCellId }
    }

    return ImportPreviewGridState(
        visibleDays = visibility.visibleDays,
        visibleSlotTypes = visibility.visibleSlotTypes,
        displayedCells = visibility.displayedCells,
        cellsByDayAndSlot = cellsByDayAndSlot,
        filledCountByDay = filledCountByDay,
        editingCell = editingCell
    )
}

internal fun resolveImportPreviewVisibility(
    uiState: ImportFileUiState
): ImportPreviewVisibility {
    val requestedDays = uiState.selectedPreviewDay?.let(::listOf) ?: WeekDay.orderedValues()
    val availableCells = uiState.editableCells.filter { cell -> cell.dayOfWeek in requestedDays }
    val filledCells = availableCells.filter { cell -> cell.mealText.isNotBlank() }
    val displayedCells = if (uiState.showOnlyFilledSlots) filledCells else availableCells

    val visibleDays = when {
        uiState.selectedPreviewDay != null -> requestedDays
        !uiState.showOnlyFilledSlots -> requestedDays
        else -> requestedDays
            .filter { day -> displayedCells.any { cell -> cell.dayOfWeek == day } }
            .ifEmpty { requestedDays }
    }

    val visibleSlotTypes = if (!uiState.showOnlyFilledSlots) {
        PreviewSlotOrder
    } else {
        PreviewSlotOrder
            .filter { slotType -> displayedCells.any { cell -> cell.mealSlotType == slotType } }
            .ifEmpty { PreviewSlotOrder }
    }

    return ImportPreviewVisibility(
        visibleDays = visibleDays,
        visibleSlotTypes = visibleSlotTypes,
        availableCells = availableCells,
        displayedCells = displayedCells
    )
}
