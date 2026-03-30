package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal fun buildLoadResultState(
    stateFactory: WeeklyPlanStateFactory,
    previousState: WeeklyPlanUiState,
    snapshot: WeeklyPlanSnapshot?,
    referenceDay: WeekDay,
    hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
): WeeklyPlanUiState {
    return if (snapshot == null) {
        stateFactory.emptyLoadedState(
            previousState = previousState,
            referenceDay = referenceDay
        )
    } else {
        stateFactory.snapshotState(
            snapshot = snapshot,
            previousState = previousState,
            payload = WeeklyPlanSnapshotStatePayload(
                currentWeekReferenceDay = referenceDay
            ),
            hydrationSnapshot = hydrationSnapshot
        )
    }
}

internal fun buildLoadFailureState(
    stateFactory: WeeklyPlanStateFactory,
    previousState: WeeklyPlanUiState,
    referenceDay: WeekDay,
    throwable: Throwable
): WeeklyPlanUiState {
    return stateFactory.errorState(
        previousState = previousState,
        referenceDay = referenceDay,
        message = throwable.message
            ?: "Errore sconosciuto durante il caricamento del piano."
    )
}

internal fun buildMessagePayload(
    actionMessage: String
): WeeklyPlanSnapshotStatePayload {
    return WeeklyPlanSnapshotStatePayload(
        actionMessage = actionMessage
    )
}

internal fun buildCalorieSyncPayload(
    actionMessage: String,
    eventId: Long,
    consumptionId: String,
    mealText: String,
    mealSlotLabel: String,
    targetDayOfWeek: WeekDay
): WeeklyPlanSnapshotStatePayload {
    return WeeklyPlanSnapshotStatePayload(
        actionMessage = actionMessage,
        pendingCalorieSyncEvent = WeeklyPlanCalorieSyncUi(
            id = eventId,
            consumptionId = consumptionId,
            mealText = mealText,
            mealSlotLabel = mealSlotLabel,
            targetDayOfWeek = targetDayOfWeek,
            targetDayKey = dayKeyForCurrentWeek(targetDayOfWeek)
        )
    )
}

internal fun buildCalorieUndoPayload(
    actionMessage: String,
    eventId: Long,
    consumptionId: String
): WeeklyPlanSnapshotStatePayload {
    return WeeklyPlanSnapshotStatePayload(
        actionMessage = actionMessage,
        pendingCalorieUndoEvent = WeeklyPlanCalorieUndoUi(
            id = eventId,
            consumptionId = consumptionId
        )
    )
}
