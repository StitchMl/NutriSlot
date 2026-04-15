package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.mergeMealTextWithNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.normalizeNutritionSummary
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotDialogUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest

/**
 * Saves the current edit only for the active rendered week, keeping the base imported plan untouched.
 */
internal fun WeeklyPlanViewModel.saveEditSlotInternal(request: EditSlotSaveRequest) {
    val snapshot = currentSnapshot ?: return
    val dialog = mutableUiState.value.editSlotDialog ?: return
    val normalizedMealText = stripStoredMealNutrition(request.mealText)
    val normalizedNutritionText = normalizeNutritionSummary(request.nutritionText)
    val resolvedTargetSelection = resolveTargetSelectionInternal(
        dialog = dialog,
        request = request,
        normalizedMealText = normalizedMealText
    )

    if (resolvedTargetSelection.shouldCatalogWithGemini) {
        catalogTargetsWithGeminiBeforeSavingInternal(
            dialog = dialog,
            mealText = normalizedMealText
        ) { targetCanonicalKeys, targetSource, actionMessage ->
            customizationManager.saveSlotCustomization(
                planId = snapshot.plan.id,
                slotId = dialog.slotId,
                mealText = normalizedMealText,
                nutritionText = normalizedNutritionText,
                targetCanonicalKeys = targetCanonicalKeys,
                targetSource = targetSource
            )

            applyCustomizationUpdateInternal(
                snapshot = snapshot,
                actionMessage = actionMessage
            )
        }
        return
    }

    customizationManager.saveSlotCustomization(
        planId = snapshot.plan.id,
        slotId = dialog.slotId,
        mealText = normalizedMealText,
        nutritionText = normalizedNutritionText,
        targetCanonicalKeys = resolvedTargetSelection.targetCanonicalKeys,
        targetSource = resolvedTargetSelection.targetSource
    )

    applyCustomizationUpdateInternal(
        snapshot = snapshot,
        actionMessage = if (request.didUserEditConsumptionTargets) {
            "Box e target aggiornati."
        } else {
            "Box aggiornato."
        }
    )
}

/**
 * Persists the edited meal as the new base content for future weeks.
 */
internal fun WeeklyPlanViewModel.saveEditSlotForNextWeeksInternal(
    request: EditSlotSaveRequest
) {
    val snapshot = currentSnapshot ?: return
    val dialog = mutableUiState.value.editSlotDialog ?: return
    val normalizedMealText = stripStoredMealNutrition(request.mealText)
    val resolvedTargetSelection = resolveTargetSelectionInternal(
        dialog = dialog,
        request = request,
        normalizedMealText = normalizedMealText
    )
    val storedMealText = mergeMealTextWithNutritionSummary(
        mealText = request.mealText,
        nutritionSummary = request.nutritionText
    )

    if (resolvedTargetSelection.shouldCatalogWithGemini) {
        catalogTargetsWithGeminiBeforeSavingInternal(
            dialog = dialog,
            mealText = normalizedMealText
        ) { targetCanonicalKeys, targetSource, actionMessage ->
            persistBaseMealUpdateInternal(
                snapshot = snapshot,
                dialog = dialog,
                storedMealText = storedMealText,
                targetCanonicalKeys = targetCanonicalKeys,
                targetSource = targetSource,
                actionMessage = actionMessage
            )
        }
        return
    }

    persistBaseMealUpdateInternal(
        snapshot = snapshot,
        dialog = dialog,
        storedMealText = storedMealText,
        targetCanonicalKeys = resolvedTargetSelection.targetCanonicalKeys,
        targetSource = resolvedTargetSelection.targetSource,
        actionMessage = "Pasto salvato anche come base per le prossime settimane."
    )
}

/**
 * Executes the repository mutation that rewrites the imported base meal and refreshes UI state.
 */
internal fun WeeklyPlanViewModel.persistBaseMealUpdateInternal(
    snapshot: WeeklyPlanSnapshot,
    dialog: EditSlotDialogUi,
    storedMealText: String,
    targetCanonicalKeys: List<String>,
    targetSource: MealConsumptionTargetSource?,
    actionMessage: String
) {
    executePlanMutationInternal(
        fallbackErrorMessage = "Errore sconosciuto durante il salvataggio del pasto.",
        mutation = {
            mutationExecutor.updateSlotBaseMeal(
                planId = snapshot.plan.id,
                slotId = dialog.slotId,
                mealText = storedMealText,
                consumptionTargetCanonicalKeys = targetCanonicalKeys,
                consumptionTargetSource = targetSource
            )
        },
        onSuccess = { updatedSnapshot ->
            customizationManager.resetSlotCustomization(
                planId = snapshot.plan.id,
                slotId = dialog.slotId
            )

            applySnapshotUpdateInternal(
                snapshot = updatedSnapshot,
                payload = buildMessagePayload(
                    actionMessage = actionMessage
                )
            )
        }
    )
}
