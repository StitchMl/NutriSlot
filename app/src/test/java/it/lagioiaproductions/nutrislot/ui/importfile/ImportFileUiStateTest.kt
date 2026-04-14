package it.lagioiaproductions.nutrislot.ui.importfile

import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.state.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.state.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.importfile.state.asImportFailure
import it.lagioiaproductions.nutrislot.ui.importfile.state.beginFreshImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportFileUiStateTest {

    @Test
    fun beginFreshImport_clearsPreviousDraftAndKeepsSelectedFileName() {
        val previousState = populatedState()

        val refreshedState = previousState.beginFreshImport(selectedFileName = "nuovo.pdf")

        assertTrue(refreshedState.isLoading)
        assertFalse(refreshedState.isManualDraft)
        assertEquals("nuovo.pdf", refreshedState.selectedFileName)
        assertTrue(refreshedState.editableCells.isEmpty())
        assertTrue(refreshedState.warnings.isEmpty())
        assertNull(refreshedState.importedDraft)
        assertNull(refreshedState.importStatus)
        assertNull(refreshedState.errorMessage)
        assertNull(refreshedState.infoMessage)
        assertNull(refreshedState.selectedPreviewDay)
        assertFalse(refreshedState.showOnlyFilledSlots)
    }

    @Test
    fun asImportFailure_clearsPreviousDraftAndExposesAttemptedFileName() {
        val previousState = populatedState()

        val failedState = previousState.asImportFailure(
            message = "PDF non valido",
            selectedFileName = "rotto.pdf"
        )

        assertFalse(failedState.isLoading)
        assertFalse(failedState.isManualDraft)
        assertEquals("rotto.pdf", failedState.selectedFileName)
        assertTrue(failedState.editableCells.isEmpty())
        assertTrue(failedState.warnings.isEmpty())
        assertNull(failedState.importedDraft)
        assertNull(failedState.importStatus)
        assertEquals("PDF non valido", failedState.errorMessage)
        assertNull(failedState.infoMessage)
    }

    @Test
    fun hasAnyMealText_isTrueOnlyWhenAtLeastOneSlotIsFilled() {
        val emptyState = ImportFileUiState(
            editableCells = listOf(editableCell(mealText = ""))
        )
        val filledState = ImportFileUiState(
            editableCells = listOf(editableCell(mealText = "Pasta"))
        )

        assertFalse(emptyState.hasAnyMealText)
        assertTrue(filledState.hasAnyMealText)
    }

    private fun populatedState(): ImportFileUiState {
        return ImportFileUiState(
            isLoading = false,
            isManualDraft = true,
            selectedFileName = "vecchio.pdf",
            editableCells = listOf(editableCell(mealText = "Yogurt")),
            warnings = listOf(ImportWarning("warning")),
            importStatus = ImportStatus.PARTIAL,
            errorMessage = "errore vecchio",
            infoMessage = "info vecchia",
            selectedPreviewDay = WeekDay.MONDAY,
            showOnlyFilledSlots = true
        )
    }

    private fun editableCell(
        mealText: String
    ): EditableImportedMealCellUi {
        return EditableImportedMealCellUi(
            id = "cell",
            dayOfWeek = WeekDay.MONDAY,
            mealSlotType = MealSlotType.BREAKFAST,
            mealText = mealText,
            originalMealText = mealText,
            originalRecognitionState = CellRecognitionState.EMPTY
        )
    }
}
