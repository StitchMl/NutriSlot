package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal data class WeeklyPlanUndoMutationResult(
    val removedConsumptionId: String,
    val updatedSnapshot: WeeklyPlanSnapshot
)

internal data class WeeklyPlanConsumptionMutationResult(
    val consumptionId: String,
    val updatedSnapshot: WeeklyPlanSnapshot
)

internal class WeeklyPlanMutationExecutor(
    private val repository: WeeklyPlanRepository
) {

    suspend fun loadLatestSnapshot(): WeeklyPlanSnapshot? {
        return repository.getLatestWeeklyPlanSnapshot()
    }

    suspend fun assignReplacement(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ): WeeklyPlanSnapshot {
        repository.assignMealToSlot(
            planId = planId,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        return loadRequiredSnapshot(
            planId = planId,
            failureMessage = "Impossibile ricaricare il piano dopo l'aggiornamento."
        )
    }

    suspend fun assignCatalogOption(
        planId: String,
        targetSlotId: String,
        optionId: String
    ): WeeklyPlanSnapshot {
        repository.assignCatalogOptionToSlot(
            planId = planId,
            targetSlotId = targetSlotId,
            optionId = optionId
        )

        return loadRequiredSnapshot(
            planId = planId,
            failureMessage = "Impossibile ricaricare il piano dopo l'aggiornamento."
        )
    }

    suspend fun recordConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String
    ): WeeklyPlanConsumptionMutationResult {
        val newConsumption = repository.recordMealConsumption(
            planId = planId,
            targetSlotId = targetSlotId,
            sourceSlotId = sourceSlotId
        )

        return WeeklyPlanConsumptionMutationResult(
            consumptionId = newConsumption.id,
            updatedSnapshot = loadRequiredSnapshot(
                planId = planId,
                failureMessage = "Impossibile ricaricare il piano dopo l'aggiornamento."
            )
        )
    }

    suspend fun undoConsumption(
        planId: String,
        targetSlotId: String
    ): WeeklyPlanUndoMutationResult {
        val removedConsumptionId = repository.undoMealConsumption(
            planId = planId,
            targetSlotId = targetSlotId
        )

        return WeeklyPlanUndoMutationResult(
            removedConsumptionId = removedConsumptionId,
            updatedSnapshot = loadRequiredSnapshot(
                planId = planId,
                failureMessage = "Impossibile ricaricare il piano dopo l'annullamento."
            )
        )
    }

    private suspend fun loadRequiredSnapshot(
        planId: String,
        failureMessage: String
    ): WeeklyPlanSnapshot {
        return repository.getWeeklyPlanSnapshot(planId)
            ?: throw IllegalStateException(failureMessage)
    }
}
