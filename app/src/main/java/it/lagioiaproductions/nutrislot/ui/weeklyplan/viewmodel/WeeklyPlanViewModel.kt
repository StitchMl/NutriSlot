package it.lagioiaproductions.nutrislot.ui.weeklyplan

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
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.consumePendingCalorieSyncEventInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.consumePendingCalorieUndoEventInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.dismissEditSlotInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.loadLatestPlanInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.observeHydrationPreferencesInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.openEditSlotInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.recalculateEditSlotNutritionWithGeminiInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.resetEditSlotInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.saveEditSlotForNextWeeksInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.saveEditSlotInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.selectCalendarDayInternal
import it.lagioiaproductions.nutrislot.ui.weeklyplan.viewmodel.toggleConsumedSlotsVisibilityInternal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

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

    fun toggleSlotCompletedFromCalendar(slotId: String) =
        toggleSlotCompletedFromCalendarInternal(slotId)

    fun loadLatestPlan() = loadLatestPlanInternal()

    fun consumePendingCalorieUndoEvent() = consumePendingCalorieUndoEventInternal()

    fun selectCalendarDay(day: WeekDay) = selectCalendarDayInternal(day)

    fun toggleConsumedSlotsVisibility() = toggleConsumedSlotsVisibilityInternal()

    fun openSlotAction(slotId: String) = openSlotActionInternal(slotId)

    fun dismissSlotAction() = dismissSlotActionInternal()

    fun openEditSlot(slotId: String) = openEditSlotInternal(slotId)

    fun dismissEditSlot() = dismissEditSlotInternal()

    fun saveEditSlot(request: EditSlotSaveRequest) = saveEditSlotInternal(request)

    fun saveEditSlotForNextWeeks(request: EditSlotSaveRequest) =
        saveEditSlotForNextWeeksInternal(request)

    fun recalculateEditSlotNutritionWithGemini(mealText: String) =
        recalculateEditSlotNutritionWithGeminiInternal(mealText)

    fun resetEditSlot() = resetEditSlotInternal()

    fun consumeAsPlanned() = consumeAsPlannedInternal()

    fun consumeReplacement(sourceSlotId: String) =
        consumeReplacementInternal(sourceSlotId)

    fun selectExtraCatalogOption(optionId: String) =
        selectExtraCatalogOptionInternal(optionId)

    fun undoCompletedMeal() = undoCompletedMealInternal()

    fun consumePendingCalorieSyncEvent() = consumePendingCalorieSyncEventInternal()
}
