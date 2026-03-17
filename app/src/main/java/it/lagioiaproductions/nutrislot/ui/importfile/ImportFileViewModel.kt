package it.lagioiaproductions.nutrislot.ui.importfile

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.importer.PdfMealPlanImporter
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.domain.model.ImportWarning
import it.lagioiaproductions.nutrislot.domain.model.ImportedPlanDraft
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val filteredEditableCells: List<EditableImportedMealCellUi>
        get() = editableCells
            .filter { selectedPreviewDay == null || it.dayOfWeek == selectedPreviewDay }
            .filter { !showOnlyFilledSlots || it.mealText.isNotBlank() }
}

class ImportFileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val importer = PdfMealPlanImporter()

    private val repository = WeeklyPlanRepository(
        weeklyPlanDao = NutriSlotDatabase
            .getInstance(application)
            .weeklyPlanDao()
    )

    private val _uiState = MutableStateFlow(ImportFileUiState())
    val uiState: StateFlow<ImportFileUiState> = _uiState.asStateFlow()

    fun importFromUri(uri: Uri) {
        val context = getApplication<Application>()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            runCatching {
                val fileName = resolveDisplayName(context, uri) ?: "piano_alimentare.pdf"
                val draft = withContext(Dispatchers.IO) {
                    importer.importFromUri(
                        context = context,
                        uri = uri,
                        sourceFileName = fileName
                    )
                }
                fileName to draft
            }.onSuccess { (fileName, draft) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedFileName = fileName,
                        importedDraft = draft,
                        editableCells = draft.toEditableUiCells(),
                        warnings = draft.warnings,
                        importStatus = draft.status,
                        errorMessage = null,
                        infoMessage = buildImportInfoMessage(draft),
                        selectedPreviewDay = null,
                        showOnlyFilledSlots = false
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'import del file.",
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun confirmReviewAndSave(
        onSaved: () -> Unit
    ) {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        if (currentState.editableCells.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Non ci sono dati da salvare. Importa prima un file e controlla la preview."
                )
            }
            return
        }

        val sourceFileName = currentState.selectedFileName
            ?: currentState.importedDraft?.sourceFileName
            ?: "piano_alimentare.pdf"

        val reviewedCells = currentState.editableCells.map { cell ->
            ReviewedImportedMealCell(
                dayOfWeek = cell.dayOfWeek,
                mealSlotType = cell.mealSlotType,
                mealText = cell.mealText
            )
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.saveReviewedImport(
                        sourceFileName = sourceFileName,
                        cells = reviewedCells
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedFileName = sourceFileName,
                        importedDraft = null,
                        editableCells = emptyList(),
                        warnings = emptyList(),
                        importStatus = null,
                        errorMessage = null,
                        infoMessage = "Piano salvato correttamente nel database locale.",
                        selectedPreviewDay = null,
                        showOnlyFilledSlots = false
                    )
                }

                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message
                            ?: "Errore sconosciuto durante il salvataggio del piano.",
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun updateMealText(
        cellId: String,
        newValue: String
    ) {
        val normalized = newValue
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        _uiState.update { state ->
            state.copy(
                editableCells = state.editableCells.map { cell ->
                    if (cell.id == cellId) {
                        cell.copy(
                            mealText = normalized,
                            wasManuallyEdited = normalized != cell.originalMealText
                        )
                    } else {
                        cell
                    }
                }
            )
        }
    }

    fun clearMealText(cellId: String) {
        updateMealText(
            cellId = cellId,
            newValue = ""
        )
    }

    fun toggleShowOnlyFilledSlots() {
        _uiState.update { state ->
            state.copy(
                showOnlyFilledSlots = !state.showOnlyFilledSlots
            )
        }
    }

    fun togglePreviewDay(day: WeekDay?) {
        _uiState.update { state ->
            state.copy(
                selectedPreviewDay = if (state.selectedPreviewDay == day) {
                    null
                } else {
                    day
                }
            )
        }
    }

    private fun resolveDisplayName(
        context: Context,
        uri: Uri
    ): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumnIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameColumnIndex)
            }
        }

        return null
    }

    private fun buildImportInfoMessage(
        draft: ImportedPlanDraft
    ): String {
        val populatedCount = draft.cells.count { it.rawText.isNotBlank() }

        return when (draft.status) {
            ImportStatus.SUCCESS -> {
                "Import completato: $populatedCount slot valorizzati automaticamente. Controlla comunque la preview prima di confermare."
            }

            ImportStatus.PARTIAL -> {
                "Import parziale: alcune celle richiedono revisione manuale. Controlla warning e testi estratti prima di proseguire."
            }

            ImportStatus.UNSUPPORTED -> {
                "Il file non è stato riconosciuto bene. Puoi comunque ispezionare il risultato, ma il PDF potrebbe non essere adatto al parser automatico."
            }

            ImportStatus.FAILED -> {
                "Import fallito. Verifica il file e riprova."
            }
        }
    }

    private fun ImportedPlanDraft.toEditableUiCells(): List<EditableImportedMealCellUi> {
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
            .sortedWith(
                compareBy(
                    { it.dayOfWeek.sortOrder },
                    { it.mealSlotType.sortOrder }
                )
            )
    }
}