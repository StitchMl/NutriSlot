package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.applySnapshotUpdateInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.executePlanMutationInternal
import kotlinx.coroutines.flow.update

internal fun WeeklyPlanViewModel.toggleSlotCompletedFromCalendarInternal(slotId: String) {
    val slotUi = mutableUiState.value.slots.firstOrNull { it.slotId == slotId } ?: return

    if (slotUi.isActuallyCompletedThisWeek) {
        undoCompletedMealByTargetSlotIdInternal(slotId)
    } else {
        consumeSlotAsPlannedByTargetSlotIdInternal(slotId)
    }
}

internal fun WeeklyPlanViewModel.openSlotActionInternal(slotId: String) {
    val snapshot = currentSnapshot ?: return
    val dialog = snapshot.buildTargetSlotActionDialog(
        slotId = slotId,
        currentSlots = mutableUiState.value.slots
    ) ?: return

    mutableUiState.update { state ->
        state.copy(
            slotActionDialog = dialog,
            editSlotDialog = null,
            actionMessage = null,
            actionErrorMessage = null
        )
    }
}

internal fun WeeklyPlanViewModel.dismissSlotActionInternal() {
    mutableUiState.update { state ->
        state.copy(
            slotActionDialog = null,
            isApplyingSlotAction = false
        )
    }
}

internal fun WeeklyPlanViewModel.consumeAsPlannedInternal() {
    val targetSlotId = mutableUiState.value.slotActionDialog?.targetSlotId ?: return
    consumeSlotAsPlannedByTargetSlotIdInternal(targetSlotId)
}

internal fun WeeklyPlanViewModel.consumeReplacementInternal(sourceSlotId: String) {
    val snapshot = currentSnapshot ?: return
    val dialog = mutableUiState.value.slotActionDialog ?: return

    applyReplacementAssignmentInternal(
        snapshot = snapshot,
        targetSlotId = dialog.targetSlotId,
        sourceSlotId = sourceSlotId,
        successMessage = "Pasto riassegnato. Rimane nello slot finche non lo segni come completato."
    )
}

internal fun WeeklyPlanViewModel.selectExtraCatalogOptionInternal(optionId: String) {
    val snapshot = currentSnapshot ?: return
    val dialog = mutableUiState.value.slotActionDialog ?: return

    applyCatalogOptionAssignmentInternal(
        snapshot = snapshot,
        targetSlotId = dialog.targetSlotId,
        optionId = optionId,
        successMessage = "Opzione extra assegnata allo slot."
    )
}

internal fun WeeklyPlanViewModel.undoCompletedMealInternal() {
    val targetSlotId = mutableUiState.value.slotActionDialog?.targetSlotId ?: return
    undoCompletedMealByTargetSlotIdInternal(targetSlotId)
}

internal fun WeeklyPlanViewModel.consumeSlotAsPlannedByTargetSlotIdInternal(targetSlotId: String) {
    val snapshot = currentSnapshot ?: return
    val command = snapshot.buildPlannedSlotConsumptionCommand(
        targetSlotId = targetSlotId,
        currentSlots = mutableUiState.value.slots
    ) ?: return

    applyConsumptionInternal(
        snapshot = snapshot,
        targetSlotId = command.targetSlotId,
        sourceSlotId = command.sourceSlotId,
        targetDayOfWeek = command.targetDayOfWeek,
        successMessage = "Pasto segnato come completato nella settimana corrente.",
        consumedMealText = command.consumedMealText,
        consumedMealSlotLabel = command.consumedMealSlotLabel,
        usesCustomizedTargetMeal = command.usesCustomizedTargetMeal
    )
}

internal fun WeeklyPlanViewModel.undoCompletedMealByTargetSlotIdInternal(targetSlotId: String) {
    val snapshot = currentSnapshot ?: return

    executePlanMutationInternal(
        fallbackErrorMessage = "Errore sconosciuto durante l'annullamento del consumo.",
        mutation = {
            mutationExecutor.undoConsumption(
                planId = snapshot.plan.id,
                targetSlotId = targetSlotId
            )
        },
        onSuccess = { result ->
            applySnapshotUpdateInternal(
                snapshot = result.updatedSnapshot,
                payload = buildCalorieUndoPayload(
                    actionMessage = "Consumo annullato con successo.",
                    eventId = nextCalorieUndoEventId++,
                    consumptionId = result.removedConsumptionId
                )
            )
        }
    )
}

internal fun WeeklyPlanViewModel.applyReplacementAssignmentInternal(
    snapshot: WeeklyPlanSnapshot,
    targetSlotId: String,
    sourceSlotId: String,
    successMessage: String
) {
    executePlanMutationInternal(
        fallbackErrorMessage = "Errore sconosciuto durante l'aggiornamento dello slot.",
        mutation = {
            mutationExecutor.assignReplacement(
                planId = snapshot.plan.id,
                targetSlotId = targetSlotId,
                sourceSlotId = sourceSlotId
            )
        },
        onSuccess = { updatedSnapshot ->
            applySnapshotUpdateInternal(
                snapshot = updatedSnapshot,
                payload = buildMessagePayload(successMessage)
            )
        }
    )
}

internal fun WeeklyPlanViewModel.applyConsumptionInternal(
    snapshot: WeeklyPlanSnapshot,
    targetSlotId: String,
    sourceSlotId: String,
    targetDayOfWeek: WeekDay,
    successMessage: String,
    consumedMealText: String,
    consumedMealSlotLabel: String,
    usesCustomizedTargetMeal: Boolean
) {
    executePlanMutationInternal(
        fallbackErrorMessage = "Errore sconosciuto durante l'aggiornamento dello slot.",
        mutation = {
            mutationExecutor.recordConsumption(
                planId = snapshot.plan.id,
                targetSlotId = targetSlotId,
                sourceSlotId = sourceSlotId,
                usesCustomizedTargetMeal = usesCustomizedTargetMeal
            )
        },
        onSuccess = { result ->
            applySnapshotUpdateInternal(
                snapshot = result.updatedSnapshot,
                payload = buildCalorieSyncPayload(
                    actionMessage = successMessage,
                    eventId = nextCalorieSyncEventId++,
                    consumptionId = result.consumptionId,
                    mealText = consumedMealText,
                    mealSlotLabel = consumedMealSlotLabel,
                    targetDayOfWeek = targetDayOfWeek
                )
            )
        }
    )
}

internal fun WeeklyPlanViewModel.applyCatalogOptionAssignmentInternal(
    snapshot: WeeklyPlanSnapshot,
    targetSlotId: String,
    optionId: String,
    successMessage: String
) {
    executePlanMutationInternal(
        fallbackErrorMessage = "Errore sconosciuto durante l'assegnazione dell'opzione extra.",
        mutation = {
            mutationExecutor.assignCatalogOption(
                planId = snapshot.plan.id,
                targetSlotId = targetSlotId,
                optionId = optionId
            )
        },
        onSuccess = { updatedSnapshot ->
            applySnapshotUpdateInternal(
                snapshot = updatedSnapshot,
                payload = buildMessagePayload(successMessage)
            )
        }
    )
}
