@file:Suppress("unused")

package it.lagioiaproductions.nutrislot.ui.importfile

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.ImportedMealOption
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

    val populatedEditableCellsCount: Int
        get() = editableCells.count { it.mealText.isNotBlank() }

    @Suppress("unused")
    val emptyEditableCellsCount: Int
        get() = editableCells.count { it.mealText.isBlank() }

    val editedCellsCount: Int
        get() = editableCells.count { it.wasManuallyEdited }

    val additionalOptionsCount: Int
        get() = importedDraft?.additionalOptions?.size ?: 0

    val mealRulesCount: Int
        get() = importedDraft?.mealRules?.size ?: 0

    val additionalOptions: List<ImportedMealOption>
        get() = importedDraft?.additionalOptions.orEmpty()

    val mealRules: List<ImportedMealRule>
        get() = importedDraft?.mealRules.orEmpty()

    val filteredEditableCells: List<EditableImportedMealCellUi>
        get() = editableCells
            .filter { selectedPreviewDay == null || it.dayOfWeek == selectedPreviewDay }
            .filter { !showOnlyFilledSlots || it.mealText.isNotBlank() }
}