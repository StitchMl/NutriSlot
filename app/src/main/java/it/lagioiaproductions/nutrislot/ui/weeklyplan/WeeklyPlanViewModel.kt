package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lagioiaproductions.nutrislot.BuildConfig
import it.lagioiaproductions.nutrislot.data.ai.GeminiNutritionEstimator
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.data.water.WaterPreferencesRepository
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeeklyPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeeklyPlanRepository(
        weeklyPlanDao = NutriSlotDatabase
            .getInstance(application)
            .weeklyPlanDao()
    )

    private val preferences = application.getSharedPreferences(
        WeeklyPlanPreferences.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val waterRepository = WaterPreferencesRepository(application.applicationContext)
    private val customizationManager = WeeklyPlanCustomizationManager(preferences)
    private val mutationExecutor = WeeklyPlanMutationExecutor(repository)
    private val stateFactory = WeeklyPlanStateFactory(customizationManager)
    private val nutritionEstimator = GeminiNutritionEstimator(BuildConfig.GEMINI_API_KEY)

    private var currentSnapshot: WeeklyPlanSnapshot? = null
    private var hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    private var nextCalorieSyncEventId: Long = 1L
    private var nextCalorieUndoEventId: Long = 1L

    private val _uiState = MutableStateFlow(
        stateFactory.initialState(
            showConsumedSlots = preferences.getBoolean(
                WeeklyPlanPreferences.PREF_SHOW_CONSUMED_SLOTS,
                false
            )
        )
    )
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        observeHydrationPreferences()
    }

    fun toggleSlotCompletedFromCalendar(slotId: String) {
        val slotUi = _uiState.value.slots.firstOrNull { it.slotId == slotId } ?: return

        if (slotUi.isActuallyCompletedThisWeek) {
            undoCompletedMealByTargetSlotId(slotId)
        } else {
            consumeSlotAsPlannedByTargetSlotId(slotId)
        }
    }

    fun loadLatestPlan() {
        viewModelScope.launch {
            _uiState.update { state ->
                stateFactory.loadingState(state)
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    waterRepository.ensureCurrentDay()
                    mutationExecutor.loadLatestSnapshot() to waterRepository.preferencesFlow
                        .first()
                        .toChecklistHydrationSnapshot()
                }
            }.onSuccess { (snapshot, latestHydrationSnapshot) ->
                currentSnapshot = snapshot
                hydrationSnapshot = latestHydrationSnapshot

                val today = currentWeekDay()
                _uiState.value = buildLoadResultState(
                    stateFactory = stateFactory,
                    previousState = _uiState.value,
                    snapshot = snapshot,
                    referenceDay = today,
                    hydrationSnapshot = latestHydrationSnapshot
                )
            }.onFailure { throwable ->
                currentSnapshot = null

                _uiState.value = buildLoadFailureState(
                    stateFactory = stateFactory,
                    previousState = _uiState.value,
                    referenceDay = currentWeekDay(),
                    throwable = throwable
                )
            }
        }
    }

    fun consumePendingCalorieUndoEvent() {
        _uiState.update { state ->
            state.copy(pendingCalorieUndoEvent = null)
        }
    }

    fun selectCalendarDay(day: WeekDay) {
        _uiState.update { state ->
            state.copy(selectedCalendarDay = day)
        }
    }

    fun toggleConsumedSlotsVisibility() {
        _uiState.update { state ->
            val nextValue = !state.showConsumedSlotsInCalendar
            preferences.edit {
                putBoolean(WeeklyPlanPreferences.PREF_SHOW_CONSUMED_SLOTS, nextValue)
            }

            state.copy(showConsumedSlotsInCalendar = nextValue)
        }
    }

    fun openSlotAction(slotId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = snapshot.buildTargetSlotActionDialog(
            slotId = slotId,
            currentSlots = _uiState.value.slots
        ) ?: return

        _uiState.update { state ->
            state.copy(
                slotActionDialog = dialog,
                editSlotDialog = null,
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

    fun openEditSlot(slotId: String) {
        val slotUi = _uiState.value.slots.firstOrNull { it.slotId == slotId } ?: return

        _uiState.update { state ->
            state.copy(
                slotActionDialog = null,
                editSlotDialog = buildEditSlotDialog(slotUi)
            )
        }
    }

    fun dismissEditSlot() {
        _uiState.update { state ->
            state.copy(editSlotDialog = null)
        }
    }

    fun saveEditSlot(
        mealText: String,
        nutritionText: String
    ) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.editSlotDialog ?: return
        val normalizedMealText = stripStoredMealNutrition(mealText)
        val normalizedNutritionText = normalizeNutritionSummary(nutritionText)

        customizationManager.saveSlotCustomization(
            planId = snapshot.plan.id,
            slotId = dialog.slotId,
            mealText = normalizedMealText,
            nutritionText = normalizedNutritionText
        )

        applyCustomizationUpdate(
            snapshot = snapshot,
            actionMessage = "Box aggiornato."
        )
    }

    fun saveEditSlotForNextWeeks(
        mealText: String,
        nutritionText: String
    ) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.editSlotDialog ?: return
        val storedMealText = mergeMealTextWithNutritionSummary(
            mealText = mealText,
            nutritionSummary = nutritionText
        )

        executePlanMutation(
            fallbackErrorMessage = "Errore sconosciuto durante il salvataggio del pasto.",
            mutation = {
                mutationExecutor.updateSlotBaseMeal(
                    planId = snapshot.plan.id,
                    slotId = dialog.slotId,
                    mealText = storedMealText
                )
            },
            onSuccess = { updatedSnapshot ->
                customizationManager.resetSlotCustomization(
                    planId = snapshot.plan.id,
                    slotId = dialog.slotId
                )

                applySnapshotUpdate(
                    snapshot = updatedSnapshot,
                    payload = buildMessagePayload(
                        actionMessage = "Pasto salvato anche come base per le prossime settimane."
                    )
                )
            }
        )
    }

    fun recalculateEditSlotNutritionWithGemini(
        mealText: String
    ) {
        val dialog = _uiState.value.editSlotDialog ?: return
        val cleanedMealText = stripStoredMealNutrition(mealText)

        if (cleanedMealText.isBlank()) {
            _uiState.update { state ->
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

        _uiState.update { state ->
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

            val latestDialog = _uiState.value.editSlotDialog ?: return@launch
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
                        nutritionText = updatedNutritionText
                    )
                }
            }

            _uiState.update { state ->
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
                rebuildCurrentStateWithLatestHydration()
            }
        }
    }

    fun resetEditSlot() {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.editSlotDialog ?: return

        customizationManager.resetSlotCustomization(
            planId = snapshot.plan.id,
            slotId = dialog.slotId
        )

        applyCustomizationUpdate(
            snapshot = snapshot,
            actionMessage = "Personalizzazione rimossa."
        )
    }

    fun consumeAsPlanned() {
        val targetSlotId = _uiState.value.slotActionDialog?.targetSlotId ?: return
        consumeSlotAsPlannedByTargetSlotId(targetSlotId)
    }

    fun consumeReplacement(sourceSlotId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyReplacementAssignment(
            snapshot = snapshot,
            targetSlotId = dialog.targetSlotId,
            sourceSlotId = sourceSlotId,
            successMessage = "Pasto riassegnato. Rimane nello slot finche non lo segni come completato."
        )
    }

    fun selectExtraCatalogOption(optionId: String) {
        val snapshot = currentSnapshot ?: return
        val dialog = _uiState.value.slotActionDialog ?: return

        applyCatalogOptionAssignment(
            snapshot = snapshot,
            targetSlotId = dialog.targetSlotId,
            optionId = optionId,
            successMessage = "Opzione extra assegnata allo slot."
        )
    }

    fun undoCompletedMeal() {
        val targetSlotId = _uiState.value.slotActionDialog?.targetSlotId ?: return
        undoCompletedMealByTargetSlotId(targetSlotId)
    }

    fun consumePendingCalorieSyncEvent() {
        _uiState.update { state ->
            state.copy(pendingCalorieSyncEvent = null)
        }
    }

    private fun consumeSlotAsPlannedByTargetSlotId(targetSlotId: String) {
        val snapshot = currentSnapshot ?: return
        val command = snapshot.buildPlannedSlotConsumptionCommand(
            targetSlotId = targetSlotId,
            currentSlots = _uiState.value.slots
        ) ?: return

        applyConsumption(
            snapshot = snapshot,
            targetSlotId = command.targetSlotId,
            sourceSlotId = command.sourceSlotId,
            targetDayOfWeek = command.targetDayOfWeek,
            successMessage = "Pasto segnato come completato nella settimana corrente.",
            consumedMealText = command.consumedMealText,
            consumedMealSlotLabel = command.consumedMealSlotLabel
        )
    }

    private fun undoCompletedMealByTargetSlotId(targetSlotId: String) {
        val snapshot = currentSnapshot ?: return

        executePlanMutation(
            fallbackErrorMessage = "Errore sconosciuto durante l'annullamento del consumo.",
            mutation = {
                mutationExecutor.undoConsumption(
                    planId = snapshot.plan.id,
                    targetSlotId = targetSlotId
                )
            },
            onSuccess = { result ->
                applySnapshotUpdate(
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

    private fun applyReplacementAssignment(
        snapshot: WeeklyPlanSnapshot,
        targetSlotId: String,
        sourceSlotId: String,
        successMessage: String
    ) {
        executePlanMutation(
            fallbackErrorMessage = "Errore sconosciuto durante l'aggiornamento dello slot.",
            mutation = {
                mutationExecutor.assignReplacement(
                    planId = snapshot.plan.id,
                    targetSlotId = targetSlotId,
                    sourceSlotId = sourceSlotId
                )
            },
            onSuccess = { updatedSnapshot ->
                applySnapshotUpdate(
                    snapshot = updatedSnapshot,
                    payload = buildMessagePayload(successMessage)
                )
            }
        )
    }

    private fun applyConsumption(
        snapshot: WeeklyPlanSnapshot,
        targetSlotId: String,
        sourceSlotId: String,
        targetDayOfWeek: WeekDay,
        successMessage: String,
        consumedMealText: String,
        consumedMealSlotLabel: String
    ) {
        executePlanMutation(
            fallbackErrorMessage = "Errore sconosciuto durante l'aggiornamento dello slot.",
            mutation = {
                mutationExecutor.recordConsumption(
                    planId = snapshot.plan.id,
                    targetSlotId = targetSlotId,
                    sourceSlotId = sourceSlotId
                )
            },
            onSuccess = { result ->
                applySnapshotUpdate(
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

    private fun applyCatalogOptionAssignment(
        snapshot: WeeklyPlanSnapshot,
        targetSlotId: String,
        optionId: String,
        successMessage: String
    ) {
        executePlanMutation(
            fallbackErrorMessage = "Errore sconosciuto durante l'assegnazione dell'opzione extra.",
            mutation = {
                mutationExecutor.assignCatalogOption(
                    planId = snapshot.plan.id,
                    targetSlotId = targetSlotId,
                    optionId = optionId
                )
            },
            onSuccess = { updatedSnapshot ->
                applySnapshotUpdate(
                    snapshot = updatedSnapshot,
                    payload = buildMessagePayload(successMessage)
                )
            }
        )
    }

    private fun applyCustomizationUpdate(
        snapshot: WeeklyPlanSnapshot,
        actionMessage: String
    ) {
        _uiState.update { state ->
            stateFactory.customizedState(
                snapshot = snapshot,
                previousState = state,
                actionMessage = actionMessage,
                hydrationSnapshot = hydrationSnapshot
            )
        }
    }

    private fun applySnapshotUpdate(
        snapshot: WeeklyPlanSnapshot,
        payload: WeeklyPlanSnapshotStatePayload
    ) {
        currentSnapshot = snapshot
        _uiState.value = stateFactory.snapshotState(
            snapshot = snapshot,
            previousState = _uiState.value,
            payload = payload,
            hydrationSnapshot = hydrationSnapshot
        )
    }

    private fun <T> executePlanMutation(
        fallbackErrorMessage: String,
        mutation: suspend () -> T,
        onSuccess: (T) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                stateFactory.actionInProgress(state)
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    mutation()
                }
            }.onSuccess { result ->
                onSuccess(result)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    stateFactory.actionFailure(
                        previousState = state,
                        throwable = throwable,
                        fallbackMessage = fallbackErrorMessage
                    )
                }
            }
        }
    }

    private fun observeHydrationPreferences() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    waterRepository.ensureCurrentDay()
                }
            }

            waterRepository.preferencesFlow.collect { storedPreferences ->
                hydrationSnapshot = storedPreferences.toChecklistHydrationSnapshot()
                rebuildCurrentStateWithLatestHydration()
            }
        }
    }

    private fun rebuildCurrentStateWithLatestHydration() {
        val snapshot = currentSnapshot ?: return

        _uiState.update { state ->
            stateFactory.snapshotState(
                snapshot = snapshot,
                previousState = state,
                payload = WeeklyPlanSnapshotStatePayload(
                    actionMessage = state.actionMessage,
                    actionErrorMessage = state.actionErrorMessage,
                    isApplyingSlotAction = state.isApplyingSlotAction,
                    slotActionDialog = state.slotActionDialog,
                    currentWeekReferenceDay = state.currentWeekReferenceDay,
                    selectedCalendarDay = state.selectedCalendarDay,
                    pendingCalorieSyncEvent = state.pendingCalorieSyncEvent,
                    pendingCalorieUndoEvent = state.pendingCalorieUndoEvent,
                    editSlotDialog = state.editSlotDialog
                ),
                hydrationSnapshot = hydrationSnapshot
            )
        }
    }
}
