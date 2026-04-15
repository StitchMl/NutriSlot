package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.ai.MealTargetCatalogCandidate
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyChecklistTargetSpec
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.buildTrackableChecklistTargetSpecs
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.isWaterTarget
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.toNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Recomputes the nutrition block for the current draft meal via Gemini and keeps UI state in sync.
 */
internal fun WeeklyPlanViewModel.recalculateEditSlotNutritionWithGeminiInternal(
    mealText: String
) {
    val dialog = mutableUiState.value.editSlotDialog ?: return
    val cleanedMealText = stripStoredMealNutrition(mealText)

    if (cleanedMealText.isBlank()) {
        mutableUiState.update { state ->
            state.copy(
                editSlotDialog = dialog.copy(
                    mealText = cleanedMealText,
                    isGeminiRecalculating = false,
                    geminiMessage = "Scrivi prima il pasto da analizzare con Gemini."
                )
            )
        }
        return
    }

    mutableUiState.update { state ->
        state.copy(
            editSlotDialog = dialog.copy(
                mealText = cleanedMealText,
                isGeminiRecalculating = true,
                geminiMessage = "Ricalcolo nutrienti in corso con Gemini..."
            )
        )
    }

    viewModelScope.launch {
        val estimateResult = withContext(Dispatchers.IO) {
            nutritionEstimator.estimateNutritionForMealDetailed(cleanedMealText)
        }

        val latestDialog = mutableUiState.value.editSlotDialog ?: return@launch
        if (latestDialog.slotId != dialog.slotId) return@launch

        val updatedNutritionText = estimateResult.nutrition
            ?.toNutritionSummary()
            ?.takeIf { it.isNotBlank() }

        if (updatedNutritionText != null) {
            currentSnapshot?.let { snapshot ->
                customizationManager.saveSlotCustomization(
                    planId = snapshot.plan.id,
                    slotId = dialog.slotId,
                    mealText = cleanedMealText,
                    nutritionText = updatedNutritionText,
                    targetCanonicalKeys = latestDialog.selectedConsumptionTargetCanonicalKeys,
                    targetSource = latestDialog.consumptionTargetSource
                )
            }
        }

        mutableUiState.update { state ->
            state.copy(
                editSlotDialog = latestDialog.copy(
                    mealText = cleanedMealText,
                    nutritionText = updatedNutritionText ?: latestDialog.nutritionText,
                    isGeminiRecalculating = false,
                    geminiMessage = if (updatedNutritionText != null) {
                        "Nutrienti aggiornati e applicati al weekly plan."
                    } else {
                        estimateResult.errorMessage
                            ?: "Gemini non ha restituito una stima valida per questo pasto."
                    }
                )
            )
        }

        if (updatedNutritionText != null) {
            rebuildCurrentStateWithLatestHydrationInternal()
        }
    }
}

/**
 * Determines whether target chips stay as-is, become manual or need a new Gemini classification pass.
 */
@Suppress("UnusedReceiverParameter")
internal fun WeeklyPlanViewModel.resolveTargetSelectionInternal(
    dialog: EditSlotDialogUi,
    request: EditSlotSaveRequest,
    normalizedMealText: String
): ResolvedTargetSelection {
    val normalizedTargetKeys = request.selectedConsumptionTargetCanonicalKeys
        .map(String::trim)
        .filter { it.isNotBlank() }
        .distinct()
        .filter { selectedKey ->
            dialog.availableConsumptionTargets.any { it.canonicalKey == selectedKey }
        }
    val originalMealText = stripStoredMealNutrition(dialog.mealText)
    val mealChanged = normalizedMealText != originalMealText
    val existingTargetKeys = dialog.selectedConsumptionTargetCanonicalKeys.distinct()
    val hasAvailableTargets = dialog.availableConsumptionTargets.isNotEmpty()

    return when {
        request.didUserEditConsumptionTargets -> {
            ResolvedTargetSelection(
                targetCanonicalKeys = normalizedTargetKeys,
                targetSource = MealConsumptionTargetSource.MANUAL,
                shouldCatalogWithGemini = false
            )
        }

        dialog.consumptionTargetSource == MealConsumptionTargetSource.MANUAL -> {
            ResolvedTargetSelection(
                targetCanonicalKeys = existingTargetKeys,
                targetSource = MealConsumptionTargetSource.MANUAL,
                shouldCatalogWithGemini = false
            )
        }

        mealChanged && normalizedMealText.isNotBlank() && hasAvailableTargets -> {
            ResolvedTargetSelection(
                targetCanonicalKeys = emptyList(),
                targetSource = null,
                shouldCatalogWithGemini = true
            )
        }

        else -> {
            ResolvedTargetSelection(
                targetCanonicalKeys = existingTargetKeys,
                targetSource = dialog.consumptionTargetSource,
                shouldCatalogWithGemini = false
            )
        }
    }
}

/**
 * Runs Gemini target classification before persisting a draft whose meal text changed materially.
 */
internal fun WeeklyPlanViewModel.catalogTargetsWithGeminiBeforeSavingInternal(
    dialog: EditSlotDialogUi,
    mealText: String,
    onResolved: (
        targetCanonicalKeys: List<String>,
        targetSource: MealConsumptionTargetSource?,
        actionMessage: String
    ) -> Unit
) {
    updateEditDialogGeminiStateInternal(
        dialog = dialog,
        isCatalogingTargets = true,
        message = "Catalogazione target in corso con Gemini..."
    )

    viewModelScope.launch {
        val targetCandidates = buildMealTargetCatalogCandidatesInternal(dialog)
        val result = withContext(Dispatchers.IO) {
            mealTargetCataloger.catalogMealTargets(
                mealText = mealText,
                candidates = targetCandidates
            )
        }

        if (result.errorMessage != null) {
            val fallbackMessage = "Gemini non disponibile: useremo il testo del pasto."
            applyGeminiTargetResolutionInternal(
                dialog = dialog,
                targetCanonicalKeys = emptyList(),
                targetSource = null,
                message = fallbackMessage
            )
            onResolved(
                emptyList(),
                null,
                "Pasto salvato. I target saranno letti dal testo del pasto."
            )
        } else {
            applyGeminiTargetResolutionInternal(
                dialog = dialog,
                targetCanonicalKeys = result.canonicalKeys,
                targetSource = MealConsumptionTargetSource.GEMINI,
                message = "Target riconosciuti da Gemini. Salvataggio in corso..."
            )
            onResolved(
                result.canonicalKeys,
                MealConsumptionTargetSource.GEMINI,
                "Pasto salvato. Target di consumo ricatalogati con Gemini."
            )
        }
    }
}

/**
 * Builds the Gemini candidate catalog by reusing checklist metadata when the latest snapshot is available.
 */
internal fun WeeklyPlanViewModel.buildMealTargetCatalogCandidatesInternal(
    dialog: EditSlotDialogUi
): List<MealTargetCatalogCandidate> {
    val snapshot = currentSnapshot ?: return dialog.availableConsumptionTargets.map { target ->
        MealTargetCatalogCandidate(
            canonicalKey = target.canonicalKey,
            title = target.title
        )
    }

    val targetSpecByKey = buildTrackableChecklistTargetSpecs(
        importedTargets = snapshot.weeklyTargets,
        slots = mutableUiState.value.slots
    ).filterNot(WeeklyChecklistTargetSpec::isWaterTarget)
        .associateBy { it.canonicalKey }

    return dialog.availableConsumptionTargets.map { target ->
        val spec = targetSpecByKey[target.canonicalKey]
        MealTargetCatalogCandidate(
            canonicalKey = target.canonicalKey,
            title = target.title,
            matchTerms = spec?.matchTerms.orEmpty(),
            ruleDescription = spec?.sourceText ?: spec?.portionText
        )
    }
}

/**
 * Updates only the Gemini-related flags of the open edit dialog.
 */
internal fun WeeklyPlanViewModel.updateEditDialogGeminiStateInternal(
    dialog: EditSlotDialogUi,
    isCatalogingTargets: Boolean,
    message: String
) {
    mutableUiState.update { state ->
        val currentDialog = state.editSlotDialog
        if (currentDialog == null || currentDialog.slotId != dialog.slotId) {
            state
        } else {
            state.copy(
                editSlotDialog = currentDialog.copy(
                    isGeminiCatalogingTargets = isCatalogingTargets,
                    geminiMessage = message
                )
            )
        }
    }
}

/**
 * Applies Gemini target output to the currently open dialog after verifying the slot still matches.
 */
internal fun WeeklyPlanViewModel.applyGeminiTargetResolutionInternal(
    dialog: EditSlotDialogUi,
    targetCanonicalKeys: List<String>,
    targetSource: MealConsumptionTargetSource?,
    message: String
) {
    mutableUiState.update { state ->
        val currentDialog = state.editSlotDialog
        if (currentDialog == null || currentDialog.slotId != dialog.slotId) {
            state
        } else {
            state.copy(
                editSlotDialog = currentDialog.copy(
                    selectedConsumptionTargetCanonicalKeys = targetCanonicalKeys,
                    consumptionTargetSource = targetSource,
                    isGeminiCatalogingTargets = false,
                    geminiMessage = message
                )
            )
        }
    }
}

/**
 * Compact summary of how target selection should be persisted after user edits.
 */
internal data class ResolvedTargetSelection(
    val targetCanonicalKeys: List<String>,
    val targetSource: MealConsumptionTargetSource?,
    val shouldCatalogWithGemini: Boolean
)
