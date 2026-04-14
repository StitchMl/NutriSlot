package it.lagioiaproductions.nutrislot.ui.importfile

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealRule
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

data class EditableImportedMealCellUi(
    val id: String,
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val mealText: String,
    val originalMealText: String,
    val originalRecognitionState: CellRecognitionState,
    val wasManuallyEdited: Boolean = false
)

data class ImportFileUiState(
    val isLoading: Boolean = false,
    val isManualDraft: Boolean = false,
    val selectedFileName: String? = null,
    val importedDraft: ImportedPlanDraft? = null,
    val editableCells: List<EditableImportedMealCellUi> = emptyList(),
    val warnings: List<ImportWarning> = emptyList(),
    val importStatus: ImportStatus? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val selectedPreviewDay: WeekDay? = null,
    val showOnlyFilledSlots: Boolean = false
) {
    val hasEditableDraft: Boolean
        get() = editableCells.isNotEmpty()

    val hasAnyMealText: Boolean
        get() = editableCells.any { it.mealText.isNotBlank() }

    val mealRules: List<ImportedMealRule>
        get() = importedDraft?.mealRules.orEmpty()
}

internal fun ImportFileUiState.beginFreshImport(
    selectedFileName: String? = null
): ImportFileUiState {
    return copy(
        isLoading = true,
        isManualDraft = false,
        selectedFileName = selectedFileName,
        importedDraft = null,
        editableCells = emptyList(),
        warnings = emptyList(),
        importStatus = null,
        errorMessage = null,
        infoMessage = null,
        selectedPreviewDay = null,
        showOnlyFilledSlots = false
    )
}

internal fun ImportFileUiState.asImportFailure(
    message: String,
    selectedFileName: String? = this.selectedFileName
): ImportFileUiState {
    return copy(
        isLoading = false,
        isManualDraft = false,
        selectedFileName = selectedFileName,
        importedDraft = null,
        editableCells = emptyList(),
        warnings = emptyList(),
        importStatus = null,
        errorMessage = message,
        infoMessage = null,
        selectedPreviewDay = null,
        showOnlyFilledSlots = false
    )
}
