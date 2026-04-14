package it.lagioiaproductions.nutrislot.ui.weeklyplan.edit

import android.content.SharedPreferences

object WeeklyPlanPreferences {
    const val PREFERENCES_NAME = "weekly_plan_preferences"
    const val PREF_SHOW_CONSUMED_SLOTS = "show_consumed_slots_in_calendar"
    private const val PREF_SLOT_MEAL_PREFIX = "slot_custom_meal"
    private const val PREF_SLOT_NUTRITION_PREFIX = "slot_custom_nutrition"
    private const val PREF_SLOT_TARGETS_PREFIX = "slot_custom_targets"
    private const val PREF_SLOT_TARGET_SOURCE_PREFIX = "slot_custom_target_source"

    fun slotMealPreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_MEAL_PREFIX}_${planId}_$slotId"
    }

    fun slotNutritionPreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_NUTRITION_PREFIX}_${planId}_$slotId"
    }

    fun slotTargetsPreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_TARGETS_PREFIX}_${planId}_$slotId"
    }

    fun slotTargetSourcePreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_TARGET_SOURCE_PREFIX}_${planId}_$slotId"
    }
}

fun SharedPreferences.readStoredPreference(key: String): String? {
    return if (contains(key)) {
        getString(key, "") ?: ""
    } else {
        null
    }
}
