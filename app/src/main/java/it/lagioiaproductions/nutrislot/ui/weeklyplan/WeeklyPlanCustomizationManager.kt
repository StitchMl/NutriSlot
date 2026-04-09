package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.content.SharedPreferences
import androidx.core.content.edit
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot

internal class WeeklyPlanCustomizationManager(
    private val preferences: SharedPreferences
) {

    fun saveSlotCustomization(
        planId: String,
        slotId: String,
        mealText: String,
        nutritionText: String
    ) {
        preferences.edit {
            putString(WeeklyPlanPreferences.slotMealPreferenceKey(planId, slotId), mealText.trim())
                .putString(WeeklyPlanPreferences.slotNutritionPreferenceKey(planId, slotId), nutritionText.trim())
        }
    }

    fun resetSlotCustomization(planId: String, slotId: String) {
        preferences.edit {
            remove(WeeklyPlanPreferences.slotMealPreferenceKey(planId, slotId))
                .remove(WeeklyPlanPreferences.slotNutritionPreferenceKey(planId, slotId))
        }
    }

    fun applyDecorations(
        snapshot: WeeklyPlanSnapshot,
        state: WeeklyPlanUiState,
        hydrationSnapshot: WeeklyChecklistHydrationSnapshot? = null
    ): WeeklyPlanUiState {
        val decoratedSlots = decorateSlots(
            snapshot = snapshot,
            slots = state.slots
        )

        return state.copy(
            slots = decoratedSlots,
            weeklyQuantityChecklist = WeeklyQuantityChecklistBuilder.build(
                slots = decoratedSlots,
                weeklyTargets = snapshot.weeklyTargets,
                referenceDay = state.currentWeekReferenceDay,
                hydrationSnapshot = hydrationSnapshot
            )
        )
    }

    fun decorateSlots(
        snapshot: WeeklyPlanSnapshot,
        slots: List<WeeklySlotUi>
    ): List<WeeklySlotUi> {
        val nutritionSummaryBySlotType = snapshot.mealRules
            .groupBy { it.mealSlotType }
            .mapValues { (_, rules) ->
                rules.firstOrNull()
                    ?.requiredComponents
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " + ")
            }

        return slots.map { slot ->
            val customMealText = preferences.readStoredPreference(
                key = WeeklyPlanPreferences.slotMealPreferenceKey(snapshot.plan.id, slot.slotId)
            )
            val customNutritionText = preferences.readStoredPreference(
                key = WeeklyPlanPreferences.slotNutritionPreferenceKey(snapshot.plan.id, slot.slotId)
            )
            val displayedMealText = customMealText ?: slot.displayedMealText

            slot.copy(
                displayedMealText = displayedMealText,
                nutritionSummary = customNutritionText
                    ?: extractStoredNutritionSummary(displayedMealText)
                    ?: nutritionSummaryBySlotType[slot.mealSlotType],
                hasCustomizations = customMealText != null || customNutritionText != null
            )
        }
    }
}
