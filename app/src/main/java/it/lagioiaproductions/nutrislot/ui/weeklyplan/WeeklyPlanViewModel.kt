@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.domain.model.MealAssignment
import it.lagioiaproductions.nutrislot.domain.model.MealConsumption
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeeklySlotUi(
    val slotId: String,
    val dayOfWeek: WeekDay,
    val mealSlotType: MealSlotType,
    val originalMealText: String,
    val displayedMealText: String,
    val displayState: SlotDisplayState,
    val isActuallyCompletedThisWeek: Boolean,
    val reassignedFromDayLabel: String? = null,
    val reassignedFromMealSlotLabel: String? = null
)

data class ReplacementMealOptionUi(
    val sourceSlotId: String,
    val sourceDayLabel: String,
    val sourceMealSlotLabel: String,
    val mealText: String
)

data class SlotActionDialogUi(
    val targetSlotId: String,
    val targetDayLabel: String,
    val targetMealSlotLabel: String,
    val currentDisplayedMealText: String,
    val currentAssignedSourceSlotId: String?,
    val targetDisplayState: SlotDisplayState,
    val isTargetActuallyCompletedThisWeek: Boolean,
    val reassignedFromDayLabel: String? = null,
    val reassignedFromMealSlotLabel: String? = null,
    val canConsumeAsPlanned: Boolean,
    val replacementOptions: List<ReplacementMealOptionUi>
)

data class WeeklyPlanUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val planId: String? = null,
    val planTitle: String? = null,
    val sourceFileName: String? = null,
    val currentWeekReferenceDay: WeekDay = currentWeekDay(),
    val selectedCalendarDay: WeekDay = currentWeekDay(),
    val showConsumedSlotsInCalendar: Boolean = false,
    val slots: List<WeeklySlotUi> = emptyList(),
    val slotActionDialog: SlotActionDialogUi? = null,
    val isApplyingSlotAction: Boolean = false,
    val actionMessage: String? = null,
    val actionErrorMessage: String? = null,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = hasLoadedOnce && slots.isEmpty() && errorMessage == null

    val populatedSlotsCount: Int
        get() = slots.count { it.originalMealText.isNotBlank() }

    val orderedCalendarDays: List<WeekDay>
        get() {
            val ordered = WeekDay.orderedValues()
            val startIndex = ordered.indexOf(currentWeekReferenceDay).coerceAtLeast(0)
            return ordered.drop(startIndex) + ordered.take(startIndex)
        }
}

class WeeklyPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeeklyPlanRepository(
        weeklyPlanDao = NutriSlotDatabase
            .getInstance(application)
            .weeklyPlanDao()
    )

    private var currentSnapshot: WeeklyPlanSnapshot? = null

    private val _uiState = MutableStateFlow(
        WeeklyPlanUiState(
            isLoading = true
        )
    )
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    fun loadLatestPlan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.getLatestWeeklyPlanSnapshot()
                }
            }.onSuccess { snapshot ->
                currentSnapshot = snapshot

                val today = currentWeekDay()
                val currentShowConsumed = _uiState.value.showConsumedSlotsInCalendar
                val currentSelectedDay = _uiState.value.selectedCalendarDay

                if (snapshot == null) {
                    _uiState.value = WeeklyPlanUiState(
                        isLoading = false,
                        hasLoadedOnce = true,
                        currentWeekReferenceDay = today,
                        selectedCalendarDay = today,
                        showConsumedSlotsInCalendar = currentShowConsumed,
                        slots = emptyList(),
                        errorMessage = null
                    )
                } else {
                    _uiState.value = snapshot.toUiState(
                        actionMessage = null,
                        actionErrorMessage = null,
                        isApplyingSlotAction = false,
                        slotActionDialog = null,
                        currentWeekReferenceDay = today,
                        selectedCalendarDay = currentSelectedDay,
                        showConsumedSlotsInCalendar = currentShowConsumed
                    )
                }
            }.onFailure { throwable ->
                currentSnapshot = null

                _uiState.value = WeeklyPlanUiState(
                    isLoading = false,
                    hasLoadedOnce = true,
                    currentWeekReferenceDay = currentWeekDay(),
                    selectedCalendarDay = currentWeekDay(),
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar,
                    slots = emptyList(),
                    errorMessage = throwable.message
                        ?: "Errore sconosciuto durante il caricamento del piano."
                )
            }
        }
    }

    fun selectCalendarDay(day: WeekDay) {
        _uiState.update { state ->
            state.copy(selectedCalendarDay = day)
        }
    }

    fun toggleConsumedSlotsVisibility() {
        _uiState.update { state ->
            state.copy(
                showConsumedSlotsInCalendar = !state.showConsumedSlotsInCalendar
            )
        }
    }

    fun openSlotAction(slotId: String) {
        val snapshot = currentSnapshot ?: return
        val targetUi = buildWeeklySlotUis(snapshot)
            .firstOrNull { it.slotId == slotId }
            ?: return

        val dialog = buildSlotActionDialog(
            snapshot = snapshot,
            targetUi = targetUi
        )

        _uiState.update { state ->
            state.copy(
                slotActionDialog = dialog,
                actionMessage = null,
                actionErrorMessage = null
            )
        }
    }

    fun dismissSlotAction() {
        _uiState.update { state ->
            state.copy(
                slotActionDialog = null,
                isApplyingSlotAction = false
            )
        }
    }

    fun consumeAsPlanned() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return
        val assignedSourceSlotId = dialog.currentAssignedSourceSlotId ?: return

        applyConsumption(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            sourceSlotId = assignedSourceSlotId,
            successMessage = "Pasto segnato come completato nella settimana corrente."
        )
    }

    fun consumeReplacement(sourceSlotId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyReplacementAssignment(
            planId = snapshot.plan.id,
            targetSlotId = dialog.targetSlotId,
            sourceSlotId = sourceSlotId,
            successMessage = "Pasto riassegnato. Rimane nello slot finché non lo segni come completato."
        )
    }

    private fun applyReplacementAssignment(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String,
        successMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.assignMealToSlot(
                        planId = planId,
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )

                    repository.getWeeklyPlanSnapshot(planId)
                        ?: throw IllegalStateException("Impossibile ricaricare il piano dopo l'aggiornamento.")
                }
            }.onSuccess { updatedSnapshot ->
                currentSnapshot = updatedSnapshot

                _uiState.value = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'aggiornamento dello slot."
                    )
                }
            }
        }
    }

    private fun applyConsumption(
        planId: String,
        targetSlotId: String,
        sourceSlotId: String,
        successMessage: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingSlotAction = true,
                    actionErrorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    repository.recordMealConsumption(
                        planId = planId,
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )

                    repository.getWeeklyPlanSnapshot(planId)
                        ?: throw IllegalStateException("Impossibile ricaricare il piano dopo l'aggiornamento.")
                }
            }.onSuccess { updatedSnapshot ->
                currentSnapshot = updatedSnapshot

                _uiState.value = updatedSnapshot.toUiState(
                    actionMessage = successMessage,
                    actionErrorMessage = null,
                    isApplyingSlotAction = false,
                    slotActionDialog = null,
                    currentWeekReferenceDay = _uiState.value.currentWeekReferenceDay,
                    selectedCalendarDay = _uiState.value.selectedCalendarDay,
                    showConsumedSlotsInCalendar = _uiState.value.showConsumedSlotsInCalendar
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingSlotAction = false,
                        actionErrorMessage = throwable.message
                            ?: "Errore sconosciuto durante l'aggiornamento dello slot."
                    )
                }
            }
        }
    }

    private fun WeeklyPlanSnapshot.toUiState(
        actionMessage: String?,
        actionErrorMessage: String?,
        isApplyingSlotAction: Boolean,
        slotActionDialog: SlotActionDialogUi?,
        currentWeekReferenceDay: WeekDay,
        selectedCalendarDay: WeekDay,
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

    private fun buildWeeklySlotUis(
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
            .sortedWith(
                compareBy(
                    { it.dayOfWeek.sortOrder },
                    { it.mealSlotType.sortOrder }
                )
            )
    }

    private fun buildSlotActionDialog(
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
                .sortedWith(
                    compareBy(
                        { it.dayOfWeek.sortOrder },
                        { it.mealSlotType.sortOrder }
                    )
                )
                .map { sourceSlot ->
                    ReplacementMealOptionUi(
                        sourceSlotId = sourceSlot.id,
                        sourceDayLabel = sourceSlot.dayOfWeek.displayName,
                        sourceMealSlotLabel = sourceSlot.mealSlotType.displayName,
                        mealText = sourceSlot.plannedMealText
                    )
                }
        }

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
            replacementOptions = replacementOptions
        )
    }

    private fun isSourceAvailableForTarget(
        snapshot: WeeklyPlanSnapshot,
        targetSlotId: String,
        candidateSourceSlotId: String
    ): Boolean {
        val planning = buildActiveWeekPlanning(snapshot)

        val sourceUsedOrReservedByAnotherTarget = planning.usedSourceByTarget.any { usage ->
            usage.sourceSlotId == candidateSourceSlotId &&
                    usage.targetSlotId != targetSlotId
        }

        return !sourceUsedOrReservedByAnotherTarget
    }

    private fun areMealSlotTypesCompatible(
        targetType: MealSlotType,
        sourceType: MealSlotType
    ): Boolean {
        if (targetType == sourceType) {
            return true
        }

        return when (targetType) {
            MealSlotType.LUNCH -> sourceType == MealSlotType.DINNER
            MealSlotType.DINNER -> sourceType == MealSlotType.LUNCH
            MealSlotType.MORNING_SNACK -> sourceType == MealSlotType.AFTERNOON_SNACK
            MealSlotType.AFTERNOON_SNACK -> sourceType == MealSlotType.MORNING_SNACK
            MealSlotType.BREAKFAST -> false
        }
    }

    private fun WeeklyPlanSnapshot.activeWeekConsumptions(): List<MealConsumption> {
        return consumptions.filter { consumption ->
            isInCurrentWeek(consumption.consumedAtEpochMillis)
        }
    }

    private fun WeeklyPlanSnapshot.activeWeekAssignments(): List<MealAssignment> {
        return assignments.filter { assignment ->
            isInCurrentWeek(assignment.assignedAtEpochMillis)
        }
    }

    private fun buildActiveWeekPlanning(
        snapshot: WeeklyPlanSnapshot
    ): ActiveWeekPlanning {
        val actualSourceByTarget = linkedMapOf<String, String>()
        snapshot.activeWeekConsumptions()
            .sortedBy { it.consumedAtEpochMillis }
            .forEach { consumption ->
                actualSourceByTarget[consumption.targetSlotId] = consumption.sourceSlotId
            }

        val pendingSourceByTarget = linkedMapOf<String, String>()
        snapshot.activeWeekAssignments()
            .sortedBy { it.assignedAtEpochMillis }
            .forEach { assignment ->
                if (!actualSourceByTarget.containsKey(assignment.targetSlotId)) {
                    pendingSourceByTarget[assignment.targetSlotId] = assignment.sourceSlotId
                }
            }

        val usedSourceByTarget = buildList {
            actualSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
                add(
                    SourceUsage(
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )
                )
            }

            pendingSourceByTarget.forEach { (targetSlotId, sourceSlotId) ->
                add(
                    SourceUsage(
                        targetSlotId = targetSlotId,
                        sourceSlotId = sourceSlotId
                    )
                )
            }
        }

        return ActiveWeekPlanning(
            actualSourceByTarget = actualSourceByTarget,
            pendingSourceByTarget = pendingSourceByTarget,
            usedSourceByTarget = usedSourceByTarget
        )
    }

    private fun isInCurrentWeek(epochMillis: Long): Boolean {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeekStart = weekStart.plusWeeks(1)

        return !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
    }
}

private data class SourceUsage(
    val targetSlotId: String,
    val sourceSlotId: String
)

private data class ActiveWeekPlanning(
    val actualSourceByTarget: Map<String, String>,
    val pendingSourceByTarget: Map<String, String>,
    val usedSourceByTarget: List<SourceUsage>
)

private fun currentWeekDay(): WeekDay {
    return when (LocalDate.now(ZoneId.systemDefault()).dayOfWeek) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }
}