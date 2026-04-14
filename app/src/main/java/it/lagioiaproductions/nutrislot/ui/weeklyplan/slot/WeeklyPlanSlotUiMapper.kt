package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import it.lagioiaproductions.nutrislot.domain.model.MealSlot
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar.buildActiveWeekPlanning
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager

internal fun buildWeeklySlotUis(
    snapshot: WeeklyPlanSnapshot,
    customizationManager: WeeklyPlanCustomizationManager
): List<WeeklySlotUi> {
    return WeeklySlotUiMapper(snapshot, customizationManager).build()
}

private class WeeklySlotUiMapper(
    private val snapshot: WeeklyPlanSnapshot,
    private val customizationManager: WeeklyPlanCustomizationManager
) {
    private val planning = buildActiveWeekPlanning(snapshot)
    private val slotById = snapshot.slots.associateBy(MealSlot::id)
    private val actualUsedSourceIds = planning.actualSourceByTarget.values.toSet()
    private val pendingReservedSourceIds = planning.pendingSourceByTarget.values.toSet()

    fun build(): List<WeeklySlotUi> {
        return snapshot.slots
            .map(::mapSlot)
            .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))
    }

    private fun mapSlot(slot: MealSlot): WeeklySlotUi {
        val actualSourceSlotId = planning.actualSourceByTarget[slot.id]
        val pendingSourceSlotId = resolvePendingSourceSlotId(slot.id, actualSourceSlotId)
        val reassignedFromSlot = pendingSourceSlotId?.let(slotById::get)

        return WeeklySlotUi(
            slotId = slot.id,
            dayOfWeek = slot.dayOfWeek,
            mealSlotType = slot.mealSlotType,
            originalMealText = slot.plannedMealText,
            displayedMealText = resolveDisplayedMealText(
                slot,
                actualSourceSlotId,
                pendingSourceSlotId
            ),
            displayedConsumptionTargetCanonicalKeys = resolveDisplayedConsumptionTargetCanonicalKeys(
                slot = slot,
                actualSourceSlotId = actualSourceSlotId,
                pendingSourceSlotId = pendingSourceSlotId
            ),
            displayedConsumptionTargetSource = resolveDisplayedConsumptionTargetSource(
                slot = slot,
                actualSourceSlotId = actualSourceSlotId,
                pendingSourceSlotId = pendingSourceSlotId
            ),
            displayState = resolveDisplayState(slot, actualSourceSlotId),
            isActuallyCompletedThisWeek = actualSourceSlotId != null,
            reassignedFromDayLabel = reassignedFromSlot?.dayOfWeek?.displayName,
            reassignedFromMealSlotLabel = reassignedFromSlot?.mealSlotType?.displayName,
            hasCustomizations = hasCustomizations(slot)
        )
    }

    private fun hasCustomizations(slot: MealSlot): Boolean {
        return customizationManager.hasSlotCustomization(snapshot.plan.id, slot.id)
    }

    private fun resolvePendingSourceSlotId(
        slotId: String,
        actualSourceSlotId: String?
    ): String? {
        return if (actualSourceSlotId == null) {
            planning.pendingSourceByTarget[slotId]
        } else {
            null
        }
    }

    private fun resolveDisplayedMealText(
        slot: MealSlot,
        actualSourceSlotId: String?,
        pendingSourceSlotId: String?
    ): String {
        return when {
            actualSourceSlotId != null -> {
                slotById[actualSourceSlotId]?.plannedMealText.orEmpty()
            }

            pendingSourceSlotId != null -> {
                slotById[pendingSourceSlotId]?.plannedMealText.orEmpty()
            }

            else -> {
                slot.plannedMealText
            }
        }
    }

    private fun resolveDisplayedConsumptionTargetCanonicalKeys(
        slot: MealSlot,
        actualSourceSlotId: String?,
        pendingSourceSlotId: String?
    ): List<String> {
        return resolveDisplayedConsumptionTargetSourceSlot(
            slot = slot,
            actualSourceSlotId = actualSourceSlotId,
            pendingSourceSlotId = pendingSourceSlotId
        )?.consumptionTargetCanonicalKeys.orEmpty()
    }

    private fun resolveDisplayedConsumptionTargetSource(
        slot: MealSlot,
        actualSourceSlotId: String?,
        pendingSourceSlotId: String?
    ) = resolveDisplayedConsumptionTargetSourceSlot(
        slot = slot,
        actualSourceSlotId = actualSourceSlotId,
        pendingSourceSlotId = pendingSourceSlotId
    )?.consumptionTargetSource

    private fun resolveDisplayedConsumptionTargetSourceSlot(
        slot: MealSlot,
        actualSourceSlotId: String?,
        pendingSourceSlotId: String?
    ): MealSlot? {
        return when {
            actualSourceSlotId != null -> slotById[actualSourceSlotId]
            pendingSourceSlotId != null -> slotById[pendingSourceSlotId]
            else -> slot
        }
    }

    private fun resolveDisplayState(
        slot: MealSlot,
        actualSourceSlotId: String?
    ): SlotDisplayState {
        return when {
            actualSourceSlotId != null && actualSourceSlotId == slot.id -> {
                SlotDisplayState.ConsumedAsPlanned
            }

            actualSourceSlotId != null -> {
                SlotDisplayState.ConsumedWithReplacement(
                    sourceSlotId = actualSourceSlotId
                )
            }

            slot.plannedMealText.isBlank() -> {
                SlotDisplayState.Empty
            }

            slot.id in actualUsedSourceIds || slot.id in pendingReservedSourceIds -> {
                SlotDisplayState.OriginalMealAlreadyUsedElsewhere
            }

            else -> {
                SlotDisplayState.PlannedAvailable
            }
        }
    }
}
