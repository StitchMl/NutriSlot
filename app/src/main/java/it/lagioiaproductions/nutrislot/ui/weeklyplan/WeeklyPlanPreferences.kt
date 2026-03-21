package it.lagioiaproductions.nutrislot.ui.weeklyplan

import android.content.SharedPreferences

object WeeklyPlanPreferences {
    const val PREFERENCES_NAME = "weekly_plan_preferences"
    const val PREF_SHOW_CONSUMED_SLOTS = "show_consumed_slots_in_calendar"
    private const val PREF_SLOT_MEAL_PREFIX = "slot_custom_meal"
    private const val PREF_SLOT_NUTRITION_PREFIX = "slot_custom_nutrition"

    fun slotMealPreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_MEAL_PREFIX}_${planId}_$slotId"
    }

    fun slotNutritionPreferenceKey(planId: String, slotId: String): String {
        return "${PREF_SLOT_NUTRITION_PREFIX}_${planId}_$slotId"
    }
}

fun SharedPreferences.readStoredPreference(key: String): String? {
    return if (contains(key)) {
        getString(key, "") ?: ""
    } else {
        null
    }
}