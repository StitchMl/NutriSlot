package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import android.content.SharedPreferences
import androidx.core.content.edit
import it.lagioiaproductions.nutrislot.data.repository.mapper.deserializeStringList
import it.lagioiaproductions.nutrislot.data.repository.mapper.serializeStringList
import it.lagioiaproductions.nutrislot.domain.model.MealConsumptionTargetSource
import it.lagioiaproductions.nutrislot.domain.model.WeeklyPlanSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyChecklistHydrationSnapshot
import it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist.WeeklyQuantityChecklistBuilder
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.state.WeeklyPlanUiState

/**
 * Persists local, user-driven weekly plan customizations outside the imported base snapshot.
 */
internal class WeeklyPlanCustomizationManager(
    private val preferences: SharedPreferences
) {

    /**
     * Stores the current slot customization so it can be reapplied on the next state rebuild.
     */
    @Suppress("unused")
    fun saveSlotCustomization(
        planId: String,
        slotId: String,
        mealText: String,
        nutritionText: String,
        targetCanonicalKeys: List<String>,
        targetSource: MealConsumptionTargetSource?
    ) {
        preferences.edit {
            putString(WeeklyPlanPreferences.slotMealPreferenceKey(planId, slotId), mealText.trim())
                .putString(WeeklyPlanPreferences.slotNutritionPreferenceKey(planId, slotId), nutritionText.trim())
                .putString(
                    WeeklyPlanPreferences.slotTargetsPreferenceKey(planId, slotId),
                    serializeStringList(targetCanonicalKeys)
                )
                .putString(
                    WeeklyPlanPreferences.slotTargetSourcePreferenceKey(planId, slotId),
                    targetSource?.name
                )
        }
    }

    /**
     * Clears all locally stored overrides for the provided slot.
     */
    @Suppress("unused")
    fun resetSlotCustomization(planId: String, slotId: String) {
        preferences.edit {
            remove(WeeklyPlanPreferences.slotMealPreferenceKey(planId, slotId))
                .remove(WeeklyPlanPreferences.slotNutritionPreferenceKey(planId, slotId))
                .remove(WeeklyPlanPreferences.slotTargetsPreferenceKey(planId, slotId))
                .remove(WeeklyPlanPreferences.slotTargetSourcePreferenceKey(planId, slotId))
        }
    }

    /**
     * Checks whether any local override is currently stored for the provided slot.
     */
    fun hasSlotCustomization(planId: String, slotId: String): Boolean {
        return preferences.contains(WeeklyPlanPreferences.slotMealPreferenceKey(planId, slotId)) ||
            preferences.contains(WeeklyPlanPreferences.slotNutritionPreferenceKey(planId, slotId)) ||
            preferences.contains(WeeklyPlanPreferences.slotTargetsPreferenceKey(planId, slotId)) ||
            preferences.contains(WeeklyPlanPreferences.slotTargetSourcePreferenceKey(planId, slotId))
    }

    /**
     * Applies local overrides to slots and rebuilds the derived checklist section.
     */
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

    /**
     * Merges stored preferences into the slot list rendered by the weekly plan UI.
     */
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
            val customTargetKeys = preferences.readStoredPreference(
                key = WeeklyPlanPreferences.slotTargetsPreferenceKey(snapshot.plan.id, slot.slotId)
            )?.let(::deserializeStringList).orEmpty()
            val customTargetSource = preferences.readStoredPreference(
                key = WeeklyPlanPreferences.slotTargetSourcePreferenceKey(snapshot.plan.id, slot.slotId)
            )?.takeIf { it.isNotBlank() }?.let(MealConsumptionTargetSource::valueOf)
            val displayedMealText = customMealText ?: slot.displayedMealText
            val usesCustomizedMealText = customMealText != null && customMealText != slot.displayedMealText
            val displayedConsumptionTargetCanonicalKeys = when {
                customTargetSource != null -> customTargetKeys
                usesCustomizedMealText -> emptyList()
                else -> slot.displayedConsumptionTargetCanonicalKeys
            }
            val displayedConsumptionTargetSource = when {
                customTargetSource != null -> customTargetSource
                usesCustomizedMealText -> null
                else -> slot.displayedConsumptionTargetSource
            }

            slot.copy(
                displayedMealText = displayedMealText,
                nutritionSummary = customNutritionText
                    ?: extractStoredNutritionSummary(displayedMealText)
                    ?: nutritionSummaryBySlotType[slot.mealSlotType],
                displayedConsumptionTargetCanonicalKeys = displayedConsumptionTargetCanonicalKeys,
                displayedConsumptionTargetSource = displayedConsumptionTargetSource,
                hasCustomizations = customMealText != null ||
                    customNutritionText != null ||
                    customTargetSource != null
            )
        }
    }
}
