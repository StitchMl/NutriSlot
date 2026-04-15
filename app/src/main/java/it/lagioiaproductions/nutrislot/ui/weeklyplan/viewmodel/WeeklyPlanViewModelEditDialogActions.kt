package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import kotlinx.coroutines.flow.update

/**
 * Opens the edit dialog starting from the slot currently rendered in UI state.
 */
internal fun WeeklyPlanViewModel.openEditSlotInternal(slotId: String) {
    val slotUi = mutableUiState.value.slots.firstOrNull { it.slotId == slotId } ?: return

    mutableUiState.update { state ->
        state.copy(
            slotActionDialog = null,
            editSlotDialog = buildEditSlotDialog(
                slotUi = slotUi,
                availableConsumptionTargets = state.weeklyQuantityChecklist.toEditableConsumptionTargets()
            )
        )
    }
}

/**
 * Closes the edit dialog without mutating the underlying snapshot.
 */
internal fun WeeklyPlanViewModel.dismissEditSlotInternal() {
    mutableUiState.update { state ->
        state.copy(editSlotDialog = null)
    }
}

/**
 * Removes the temporary customization for the currently edited slot and rebuilds the decorated state.
 */
internal fun WeeklyPlanViewModel.resetEditSlotInternal() {
    val snapshot = currentSnapshot ?: return
    val dialog = mutableUiState.value.editSlotDialog ?: return

    customizationManager.resetSlotCustomization(
        planId = snapshot.plan.id,
        slotId = dialog.slotId
    )

    applyCustomizationUpdateInternal(
        snapshot = snapshot,
        actionMessage = "Personalizzazione rimossa."
    )
}
