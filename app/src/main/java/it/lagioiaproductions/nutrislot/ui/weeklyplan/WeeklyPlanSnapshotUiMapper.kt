package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal fun WeeklyPlanSnapshot.toUiState(
    actionMessage: String?,
    actionErrorMessage: String?,
    isApplyingSlotAction: Boolean,
    slotActionDialog: SlotActionDialogUi?,
    currentWeekReferenceDay: it.lagioiaproductions.nutrislot.domain.model.WeekDay,
    selectedCalendarDay: it.lagioiaproductions.nutrislot.domain.model.WeekDay,
    showConsumedSlotsInCalendar: Boolean
): WeeklyPlanUiState {
    return WeeklyPlanUiState(
        isLoading = false,
        hasLoadedOnce = true,
        planId = plan.id,
        planTitle = plan.title,
        sourceFileName = plan.sourceFileName,
        currentWeekReferenceDay = currentWeekReferenceDay,
        selectedCalendarDay = selectedCalendarDay,
        showConsumedSlotsInCalendar = showConsumedSlotsInCalendar,
        slots = buildWeeklySlotUis(this),
        slotActionDialog = slotActionDialog,
        isApplyingSlotAction = isApplyingSlotAction,
        actionMessage = actionMessage,
        actionErrorMessage = actionErrorMessage,
        errorMessage = null
    )
}

internal fun buildWeeklySlotUis(
    snapshot: WeeklyPlanSnapshot
): List<WeeklySlotUi> {
    val planning = buildActiveWeekPlanning(snapshot)
    val slotById = snapshot.slots.associateBy { it.id }
    val actualUsedSourceIds = planning.actualSourceByTarget.values.toSet()
    val pendingReservedSourceIds = planning.pendingSourceByTarget.values.toSet()

    return snapshot.slots
        .map { slot ->
            val actualSourceSlotId = planning.actualSourceByTarget[slot.id]
            val pendingSourceSlotId = if (actualSourceSlotId == null) {
                planning.pendingSourceByTarget[slot.id]
            } else {
                null
            }

            val displayedMealText = when {
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

            val displayState = when {
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

            val reassignedFromSlot = pendingSourceSlotId?.let { slotById[it] }

            WeeklySlotUi(
                slotId = slot.id,
                dayOfWeek = slot.dayOfWeek,
                mealSlotType = slot.mealSlotType,
                originalMealText = slot.plannedMealText,
                displayedMealText = displayedMealText,
                displayState = displayState,
                isActuallyCompletedThisWeek = actualSourceSlotId != null,
                reassignedFromDayLabel = reassignedFromSlot?.dayOfWeek?.displayName,
                reassignedFromMealSlotLabel = reassignedFromSlot?.mealSlotType?.displayName
            )
        }
        .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))
}

internal fun buildSlotActionDialog(
    snapshot: WeeklyPlanSnapshot,
    targetUi: WeeklySlotUi
): SlotActionDialogUi {
    val planning = buildActiveWeekPlanning(snapshot)
    val targetSlot = snapshot.slots.first { it.id == targetUi.slotId }
    val targetIsActuallyCompleted = planning.actualSourceByTarget.containsKey(targetUi.slotId)
    val pendingAssignedSourceSlotId = planning.pendingSourceByTarget[targetUi.slotId]

    val currentAssignedSourceSlotId = when {
        targetIsActuallyCompleted -> null
        pendingAssignedSourceSlotId != null -> pendingAssignedSourceSlotId
        targetSlot.plannedMealText.isNotBlank() -> targetSlot.id
        else -> null
    }

    val canConsumeAsPlanned = !targetIsActuallyCompleted && currentAssignedSourceSlotId != null

    val replacementOptions = if (targetIsActuallyCompleted) {
        emptyList()
    } else {
        snapshot.slots
            .filter { candidateSourceSlot ->
                candidateSourceSlot.id != currentAssignedSourceSlotId &&
                        candidateSourceSlot.plannedMealText.isNotBlank() &&
                        areMealSlotTypesCompatible(
                            targetType = targetSlot.mealSlotType,
                            sourceType = candidateSourceSlot.mealSlotType
                        ) &&
                        isSourceAvailableForTarget(
                            snapshot = snapshot,
                            targetSlotId = targetSlot.id,
                            candidateSourceSlotId = candidateSourceSlot.id
                        )
            }
            .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))
            .map { sourceSlot ->
                ReplacementMealOptionUi(
                    sourceSlotId = sourceSlot.id,
                    sourceDayLabel = sourceSlot.dayOfWeek.displayName,
                    sourceMealSlotLabel = sourceSlot.mealSlotType.displayName,
                    mealText = sourceSlot.plannedMealText
                )
            }
    }

    val extraCatalogOptions = if (targetIsActuallyCompleted) {
        emptyList()
    } else {
        buildExtraCatalogOptions(
            snapshot = snapshot,
            targetSlotType = targetSlot.mealSlotType
        )
    }

    val mealRuleSummary = snapshot.mealRules
        .firstOrNull { it.mealSlotType == targetSlot.mealSlotType }
        ?.requiredComponents
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(separator = " + ")

    return SlotActionDialogUi(
        targetSlotId = targetUi.slotId,
        targetDayLabel = targetUi.dayOfWeek.displayName,
        targetMealSlotLabel = targetUi.mealSlotType.displayName,
        currentDisplayedMealText = targetUi.displayedMealText,
        currentAssignedSourceSlotId = currentAssignedSourceSlotId,
        targetDisplayState = targetUi.displayState,
        isTargetActuallyCompletedThisWeek = targetIsActuallyCompleted,
        reassignedFromDayLabel = targetUi.reassignedFromDayLabel,
        reassignedFromMealSlotLabel = targetUi.reassignedFromMealSlotLabel,
        canConsumeAsPlanned = canConsumeAsPlanned,
        replacementOptions = replacementOptions,
        extraCatalogOptions = extraCatalogOptions,
        mealRuleSummary = mealRuleSummary
    )
}

private fun buildExtraCatalogOptions(
    snapshot: WeeklyPlanSnapshot,
    targetSlotType: it.lagioiaproductions.nutrislot.domain.model.MealSlotType
): List<ExtraCatalogMealOptionUi> {
    return snapshot.mealOptions
        .filter { option ->
            areMealSlotTypesCompatible(
                targetType = targetSlotType,
                sourceType = option.mealSlotType
            )
        }
        .sortedWith(
            compareBy(
                { it.mealSlotType.sortOrder },
                { it.pageNumber ?: Int.MAX_VALUE },
                { it.title ?: "" },
                { it.mealText }
            )
        )
        .map { option ->
            ExtraCatalogMealOptionUi(
                title = option.title,
                mealText = option.mealText,
                sourceLabel = option.sourceType.name
                    .replace('_', ' ')
                    .lowercase(),
                pageNumber = option.pageNumber,
                tags = option.tags
            )
        }
        .distinctBy { option ->
            "${option.title}|${option.mealText}|${option.sourceLabel}|${option.pageNumber}"
        }
}