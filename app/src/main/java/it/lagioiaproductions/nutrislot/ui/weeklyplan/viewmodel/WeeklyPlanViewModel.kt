@file:Suppress("unused")

package it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import it.lagioiaproductions.nutrislot.BuildConfig
import it.lagioiaproductions.nutrislot.data.ai.GeminiMealTargetCataloger
import it.lagioiaproductions.nutrislot.data.ai.GeminiNutritionEstimator
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.data.water.WaterPreferencesRepository
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyChecklistHydrationSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanPreferences
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.EditSlotSaveRequest
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanMutationExecutor
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanStateFactory
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Screen-level coordinator for weekly plan UI.
 *
 * Public methods stay intentionally tiny and delegate to focused extension files grouped by concern.
 */
class WeeklyPlanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = NutriSlotDatabase.getInstance(application)
    private val repository = WeeklyPlanRepository(
        database = database
    )

    internal val preferences = application.getSharedPreferences(
        WeeklyPlanPreferences.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    internal val waterRepository = WaterPreferencesRepository(application.applicationContext)
    internal val customizationManager = WeeklyPlanCustomizationManager(preferences)
    internal val mutationExecutor = WeeklyPlanMutationExecutor(repository)
    internal val stateFactory = WeeklyPlanStateFactory(customizationManager)
    internal val nutritionEstimator = GeminiNutritionEstimator(BuildConfig.GEMINI_API_KEY)
    internal val mealTargetCataloger = GeminiMealTargetCataloger(BuildConfig.GEMINI_API_KEY)

    internal var currentSnapshot: WeeklyPlanSnapshot? = null
    internal var hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    internal var nextCalorieSyncEventId: Long = 1L
    internal var nextCalorieUndoEventId: Long = 1L
    internal val planOperationMutex = Mutex()

    private val _uiState = MutableStateFlow(
        stateFactory.initialState(
            showConsumedSlots = preferences.getBoolean(
                WeeklyPlanPreferences.PREF_SHOW_CONSUMED_SLOTS,
                false
            )
        )
    )
    internal val mutableUiState: MutableStateFlow<WeeklyPlanUiState>
        get() = _uiState
    val uiState: StateFlow<WeeklyPlanUiState> = _uiState.asStateFlow()

    init {
        observeHydrationPreferencesInternal()
    }

    /** Toggles a slot from planned to consumed directly from the calendar surface. */
    fun toggleSlotCompletedFromCalendar(slotId: String) =
        toggleSlotCompletedFromCalendarInternal(slotId)

    /** Loads the most recent weekly plan snapshot from storage. */
    fun loadLatestPlan() = loadLatestPlanInternal()

    /** Marks the current calorie undo event as consumed by the UI. */
    fun consumePendingCalorieUndoEvent() = consumePendingCalorieUndoEventInternal()

    /** Switches the calendar focus day used by the screen. */
    fun selectCalendarDay(day: WeekDay) = selectCalendarDayInternal(day)

    /** Flips the preference that controls whether consumed slots stay visible in calendar cells. */
    fun toggleConsumedSlotsVisibility() = toggleConsumedSlotsVisibilityInternal()

    /** Opens the slot action dialog for the requested slot. */
    fun openSlotAction(slotId: String) = openSlotActionInternal(slotId)

    /** Closes the slot action dialog. */
    fun dismissSlotAction() = dismissSlotActionInternal()

    /** Opens the meal edit dialog for the requested slot. */
    fun openEditSlot(slotId: String) = openEditSlotInternal(slotId)

    /** Closes the meal edit dialog. */
    fun dismissEditSlot() = dismissEditSlotInternal()

    /** Saves the edit only for the currently rendered week. */
    fun saveEditSlot(request: EditSlotSaveRequest) = saveEditSlotInternal(request)

    /** Persists the edit as the new base meal for future weeks. */
    fun saveEditSlotForNextWeeks(request: EditSlotSaveRequest) =
        saveEditSlotForNextWeeksInternal(request)

    /** Asks Gemini to recalculate the nutrition summary for the current draft meal. */
    fun recalculateEditSlotNutritionWithGemini(mealText: String) =
        recalculateEditSlotNutritionWithGeminiInternal(mealText)

    /** Removes local customization for the currently edited slot. */
    fun resetEditSlot() = resetEditSlotInternal()

    /** Consumes the meal that is currently planned for the active slot dialog. */
    fun consumeAsPlanned() = consumeAsPlannedInternal()

    /** Consumes a replacement meal coming from another slot. */
    fun consumeReplacement(sourceSlotId: String) =
        consumeReplacementInternal(sourceSlotId)

    /** Applies one of the extra catalog options to the currently active slot dialog. */
    fun selectExtraCatalogOption(optionId: String) =
        selectExtraCatalogOptionInternal(optionId)

    /** Reverts the latest completion for the active slot dialog. */
    fun undoCompletedMeal() = undoCompletedMealInternal()

    /** Marks the current calorie sync event as consumed by the UI. */
    fun consumePendingCalorieSyncEvent() = consumePendingCalorieSyncEventInternal()
}
