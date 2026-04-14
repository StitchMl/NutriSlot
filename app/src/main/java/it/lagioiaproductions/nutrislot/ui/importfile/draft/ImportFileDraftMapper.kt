package it.lagioiaproductions.nutrislot.ui.importfile.draft

import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.ui.importfile.state.EditableImportedMealCellUi

internal fun ImportedPlanDraft.toEditableUiCells(): List<EditableImportedMealCellUi> {
    return cells
        .mapNotNull { cell ->
            val day = cell.dayOfWeek ?: return@mapNotNull null
            val slot = cell.mealSlotType ?: return@mapNotNull null

            EditableImportedMealCellUi(
                id = cell.id,
                dayOfWeek = day,
                mealSlotType = slot,
                mealText = cell.rawText,
                originalMealText = cell.rawText,
                originalRecognitionState = cell.recognitionState,
                wasManuallyEdited = false
            )
        }
        .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))
}

internal fun buildManualEditableUiCells(): List<EditableImportedMealCellUi> {
    return WeekDay.orderedValues().flatMap { day ->
        MealSlotType.orderedValues().map { slot ->
            EditableImportedMealCellUi(
                id = "manual_${day.name}_${slot.name}",
                dayOfWeek = day,
                mealSlotType = slot,
                mealText = "",
                originalMealText = "",
                originalRecognitionState = CellRecognitionState.EMPTY,
                wasManuallyEdited = false
            )
        }
    }
}

internal fun buildImportInfoMessage(draft: ImportedPlanDraft): String {
    val populatedCount = draft.cells.count { it.rawText.isNotBlank() }
    val optionCount = draft.additionalOptions.size
    val ruleCount = draft.mealRules.size
    val weeklyTargetCount = draft.weeklyTargets.size

    return when (draft.status) {
        ImportStatus.SUCCESS -> {
            "Import completato: $populatedCount slot settimanali, $optionCount opzioni extra, $ruleCount regole nutrizionali e $weeklyTargetCount target di consumo rilevati. Controlla la preview prima di confermare."
        }
        ImportStatus.PARTIAL -> {
            "Import parziale: trovati $populatedCount slot, $optionCount opzioni extra, $ruleCount regole e $weeklyTargetCount target di consumo. Alcune sezioni richiedono revisione manuale."
        }
        ImportStatus.UNSUPPORTED -> {
            "Il file non è stato riconosciuto bene. Puoi comunque ispezionare il risultato, ma il PDF potrebbe non essere adatto al parser automatico."
        }
        ImportStatus.FAILED -> {
            "Import fallito. Verifica il file e riprova."
        }
    }
}
