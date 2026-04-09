package it.lagioiaproductions.nutrislot.ui.importfile

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.importer.PdfMealPlanImporter
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.work.NutritionEnrichmentWorker
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.widget.MealCalendarWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportFileViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val importer = PdfMealPlanImporter()
    private val database = NutriSlotDatabase.getInstance(application)

    private val repository = WeeklyPlanRepository(
        database = database
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

    fun confirmReviewAndSave(onSaved: () -> Unit) {
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

        val draft = currentState.importedDraft
        val sourceFileName = currentState.selectedFileName
            ?: draft?.sourceFileName
            ?: "piano_alimentare.pdf"

        val reviewedCells = currentState.editableCells.map { cell ->
            ReviewedImportedMealCell(
                dayOfWeek = cell.dayOfWeek,
                mealSlotType = cell.mealSlotType,
                mealText = cell.mealText
            )
        }

        val reviewedOptions = draft?.additionalOptions.orEmpty().map { option ->
            ReviewedImportedMealOption(
                mealSlotType = option.mealSlotType,
                title = option.title,
                mealText = option.rawText,
                sourceType = option.sourceType,
                tags = option.tags,
                pageNumber = option.pageNumber
            )
        }

        val reviewedRules = draft?.mealRules.orEmpty().map { rule ->
            ReviewedImportedMealRule(
                mealSlotType = rule.mealSlotType,
                label = rule.label,
                requiredComponents = rule.requiredComponents,
                pageNumber = rule.pageNumber
            )
        }

        val reviewedWeeklyTargets = draft?.weeklyTargets.orEmpty().map { target ->
            ReviewedImportedWeeklyFrequencyTarget(
                title = target.title,
                canonicalKey = target.canonicalKey,
                portionText = target.portionText,
                minimumTimesPerWeek = target.minimumTimesPerWeek,
                maximumTimesPerWeek = target.maximumTimesPerWeek,
                matchTerms = target.matchTerms,
                pageNumber = target.pageNumber,
                sourceText = target.sourceText
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
                        cells = reviewedCells,
                        extraOptions = reviewedOptions,
                        mealRules = reviewedRules,
                        weeklyTargets = reviewedWeeklyTargets
                    )
                }
            }.onSuccess { planId ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedFileName = sourceFileName,
                        importedDraft = null,
                        editableCells = emptyList(),
                        warnings = emptyList(),
                        importStatus = null,
                        errorMessage = null,
                        infoMessage = "Piano salvato. Stima nutrienti in corso in background.",
                        selectedPreviewDay = null,
                        showOnlyFilledSlots = false
                    )
                }

                NutritionEnrichmentWorker.enqueue(
                    context = getApplication(),
                    planId = planId
                )
                MealCalendarWidgetProvider.refresh(getApplication())

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
        updateMealText(cellId = cellId, newValue = "")
    }

    fun toggleShowOnlyFilledSlots() {
        _uiState.update { state ->
            state.copy(showOnlyFilledSlots = !state.showOnlyFilledSlots)
        }
    }

    fun togglePreviewDay(day: it.lagioiaproductions.nutrislot.domain.model.WeekDay?) {
        _uiState.update { state ->
            state.copy(
                selectedPreviewDay = if (state.selectedPreviewDay == day) null else day
            )
        }
    }

    private fun resolveDisplayName(
        context: Context,
        uri: Uri
    ): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumnIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameColumnIndex)
            }
        }

        return null
    }
}
