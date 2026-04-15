package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.local.room.MealOptionEntity
import it.lagioiaproductions.nutrislot.data.local.room.MealSlotEntity
import it.lagioiaproductions.nutrislot.data.repository.mapper.areMealSlotTypesCompatible
import it.lagioiaproductions.nutrislot.data.repository.planning.ActiveWeekPlanning
import it.lagioiaproductions.nutrislot.data.repository.planning.isActualSourceConsumed
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType

/**
 * Encapsulates business validation for weekly plan mutations so the repository stays transaction-focused.
 */
internal class WeeklyPlanMutationValidator {

    /**
     * Ensures an extra catalog option can be applied to the requested slot.
     */
    fun validateCatalogOptionAssignment(
        planning: ActiveWeekPlanning,
        targetSlot: MealSlotEntity,
        selectedOption: MealOptionEntity,
        targetSlotId: String
    ) {
        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta gia completato nella settimana corrente.")
        }

        val targetType = MealSlotType.valueOf(targetSlot.mealSlotType)
        val optionType = MealSlotType.valueOf(selectedOption.mealSlotType)

        if (!areMealSlotTypesCompatible(targetType = targetType, sourceType = optionType)) {
            throw IllegalStateException(
                "Questa opzione extra non e compatibile con lo slot ${targetSlot.mealSlotType.lowercase()}."
            )
        }

        if (selectedOption.mealText.isBlank()) {
            throw IllegalStateException("L'opzione extra selezionata non contiene un pasto valido.")
        }
    }

    /**
     * Validates that a target and source pair can be swapped without breaking slot-type compatibility.
     */
    fun validateSwitchCompatibility(
        targetSlot: MealSlotEntity,
        sourceSlot: MealSlotEntity,
        targetCurrentSourceSlot: MealSlotEntity,
        sourceCurrentSourceSlot: MealSlotEntity
    ) {
        val targetType = MealSlotType.valueOf(targetSlot.mealSlotType)
        val sourceType = MealSlotType.valueOf(sourceSlot.mealSlotType)
        val targetCurrentSourceType = MealSlotType.valueOf(targetCurrentSourceSlot.mealSlotType)
        val sourceCurrentSourceType = MealSlotType.valueOf(sourceCurrentSourceSlot.mealSlotType)

        if (!areMealSlotTypesCompatible(targetType = targetType, sourceType = sourceCurrentSourceType)) {
            throw IllegalStateException("Il pasto selezionato non e compatibile con lo slot target.")
        }

        if (
            targetCurrentSourceSlot.plannedMealText.isNotBlank() &&
            !areMealSlotTypesCompatible(targetType = sourceType, sourceType = targetCurrentSourceType)
        ) {
            throw IllegalStateException("Lo switch non e compatibile con il tipo dello slot sorgente.")
        }
    }

    /**
     * Checks whether a replacement or consumption can proceed in the current week.
     */
    fun validateReplacement(
        planning: ActiveWeekPlanning,
        targetSlot: MealSlotEntity,
        sourceSlot: MealSlotEntity,
        targetSlotId: String,
        sourceSlotId: String,
        allowsCustomizedTargetMeal: Boolean = false
    ) {
        if (planning.actualSourceByTarget.containsKey(targetSlotId)) {
            throw IllegalStateException("Questo slot risulta gia completato nella settimana corrente.")
        }

        if (!allowsCustomizedTargetMeal && planning.isActualSourceConsumed(sourceSlotId)) {
            throw IllegalStateException("Il pasto selezionato e gia stato consumato nella settimana corrente.")
        }

        if (!allowsCustomizedTargetMeal && sourceSlot.plannedMealText.isBlank()) {
            throw IllegalStateException("Lo slot sorgente non contiene un pasto disponibile.")
        }

        if (
            !areMealSlotTypesCompatible(
                targetType = MealSlotType.valueOf(targetSlot.mealSlotType),
                sourceType = MealSlotType.valueOf(sourceSlot.mealSlotType)
            )
        ) {
            throw IllegalStateException(
                "Sostituzione non consentita tra ${targetSlot.mealSlotType} e ${sourceSlot.mealSlotType}."
            )
        }
    }
}
